package org.hrana.hafez.service.update;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.android.gms.analytics.HitBuilders;

import org.hrana.hafez.Constants;
import org.hrana.hafez.R;
import org.hrana.hafez.di.BaseApplication;
import org.hrana.hafez.exception.TransferException;
import org.hrana.hafez.util.AmazonAwsUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static org.hrana.hafez.Constants.ACTION_ERROR;
import static org.hrana.hafez.Constants.APP_NEEDS_UPDATE_KEY;
import static org.hrana.hafez.Constants.BROADCAST_APP_UPDATE_FAILURE;
import static org.hrana.hafez.Constants.BROADCAST_APP_UPDATE_SUCCESS;
import static org.hrana.hafez.Constants.CATEGORY_ERROR;
import static org.hrana.hafez.Constants.DOWNLOAD_BUCKET;
import static org.hrana.hafez.Constants.DOWNLOAD_CASE_HISTORY_KEY;
import static org.hrana.hafez.Constants.DOWNLOAD_LAWYER_LIST_KEY;
import static org.hrana.hafez.Constants.NOTIFICATION_ID_APP_UPDATE;
import static org.hrana.hafez.Constants.SIMPLE_DATE_FORMAT;
import static org.hrana.hafez.Constants.VERSION_CODE;

/**
 * This is a @link{JobIntentService} responsible for handling download and update tasks.
 *
 */
public class AmazonJobService extends JobIntentService {
    private static final String TAG = "AmazonIntentService";

    public static int JOB_ID = 1012;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, AmazonJobService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        if (intent.getAction().equals(Constants.INTENT_ACTION_CHECK_VERSION_CODE)) {
            checkAppVersion();

        } else if (intent.getAction().equals(Constants.INTENT_ACTION_START_UPDATE)) {
            updateApp();
        } else if (intent.getAction().equals(Constants.INTENT_ACTION_CHECK_UPDATE_CONTENT)) {
            Log.d(TAG, "check or update content");

            // Check for new content in both legal case files and lawyer files
            // If files are out of date, downloads new files.
            checkFileVersion(DOWNLOAD_CASE_HISTORY_KEY, "Cases", "*"); // all files
            checkFileVersion(DOWNLOAD_LAWYER_LIST_KEY, "Lawyers", Constants.LAWYERS_CSV);
        }

    }

    private String getApkName() {
        try {
            ApplicationInfo appInfo = getPackageManager()
                    .getPackageInfo(getApplicationContext().getPackageName(), 0)
                    .applicationInfo;
            return appInfo.publicSourceDir;
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "PackageManager: name not found exception while attempting update--return assigned name");
        }
        return Constants.APK_FILE_NAME;
    }

    private void downloadContent(final String prefix, final String preferenceKey, final String fileName, final int version) {

        final File parentPath = getFilesDir();
        final File destination = new File(parentPath, prefix);
        if (!destination.exists()) {
            destination.mkdirs();
        }

        if (fileName.equals("*")) { // Download all files in bucket, except version code
            final TransferUtility utility = AmazonAwsUtil.getTransferUtility(this);

            Observable<String> downloadContentObservable =
                    Observable.just(AmazonAwsUtil.getS3Client(this))
                            .subscribeOn(Schedulers.io())
                            .concatMap(new Func1<AmazonS3Client, Observable<ObjectListing>>() {
                                @Override
                                public Observable<ObjectListing> call(AmazonS3Client amazonS3Client) {
                                    Log.d(TAG, "listObjects");
                                    return Observable.just(amazonS3Client.listObjects(new ListObjectsRequest()
                                            .withBucketName(DOWNLOAD_BUCKET).withPrefix(prefix)
                                            .withDelimiter("/")));
                                }
                            })
                            .flatMap(new Func1<ObjectListing, Observable<S3ObjectSummary>>() {
                                @Override
                                public Observable<S3ObjectSummary> call(ObjectListing objectListing) {
                                    objectListing.setDelimiter("/");
                                    Log.d(TAG, "objectListing found " + objectListing.getObjectSummaries().size() + " objects");
                                    return Observable.from(objectListing.getObjectSummaries());
                                }
                            })
                            .flatMap(new Func1<S3ObjectSummary, Observable<String>>() {

                                public Observable<String> setStringObservable(String in) {
                                    return Observable.just(in);
                                }

                                @Override
                                public Observable<String> call(final S3ObjectSummary s3ObjectSummary) {
                                    if (!s3ObjectSummary.getKey().endsWith("/")
                                            && !s3ObjectSummary.getKey().contains("versionCode.tmp")) { // don't re-download version file

                                        Log.d(TAG, "Object's key is " + s3ObjectSummary.getKey());

                                        final File file = new File(parentPath, s3ObjectSummary.getKey()); // key is in format subdirectory/filename.ext

                                        TransferObserver observer = utility.download(Constants.DOWNLOAD_BUCKET, s3ObjectSummary.getKey(), file);
                                        observer.setTransferListener(new TransferListener() {
                                            @Override
                                            public void onStateChanged(int id, TransferState state) {
                                                if (state == TransferState.FAILED) {
                                                    Log.e(TAG, "Transfer failed for " + file.getName());
                                                    onError(id, new TransferException(file.getName() + " not successfully downloaded"));
                                                } else if (state == TransferState.COMPLETED) {
                                                    Log.d(TAG, "Transfer " + file.getName() + " complete");
                                                    setStringObservable("Transfer " + file.getName() + " complete");
                                                }
                                            }

                                            @Override
                                            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                            }

                                            @Override
                                            public void onError(int id, Exception ex) {
                                                Log.e(TAG, "Failed for " + s3ObjectSummary.getKey() + ": " + ex.getMessage());
                                                utility.cancel(id);
                                                ((BaseApplication) getApplication()).getDefaultTracker()
                                                        .send(new HitBuilders.EventBuilder()
                                                                .setAction(ACTION_ERROR)
                                                                .setCategory(CATEGORY_ERROR)
                                                                .setLabel("Download error " + ex.getMessage())
                                                                .build());
                                            }
                                        });
                                    }
                                    else {
                                        // skip - it's the directory and not a file.
                                    }
                                    return null;
                                }
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnCompleted(new Action0() {
                                @Override
                                public void call() {
                                    Log.d(TAG, "aws download content success!");
                                    PreferenceManager.getDefaultSharedPreferences(AmazonJobService.this)
                                            .edit()
                                            .putInt(preferenceKey + VERSION_CODE, version)
                                            .putBoolean(getString(R.string.last_content_update_successful_key), true)
                                            .apply();
                                }
                            })
                            .doOnError(new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    Log.e(TAG, throwable.getMessage());
                                    throwable.printStackTrace();
                                }
                            });

            downloadContentObservable.subscribe(new Action1<String>() {
                @Override
                public void call(String s) {
                    Log.d(TAG, s);

                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    ((BaseApplication) getApplication()).getDefaultTracker()
                            .send(new HitBuilders.EventBuilder()
                            .setAction(ACTION_ERROR)
                            .setCategory(CATEGORY_ERROR)
                            .setLabel("Download error " + throwable.getMessage())
                            .build());
                    Log.e(TAG, "Download error");
                }
            });
        } else {

            final File file = new File(parentPath, fileName); // name the file the same as prev
            if (!file.exists()) {
                Log.d(TAG, "file did not exist--creating...");
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    Log.e(TAG, ex.getMessage());
                }
            }

            TransferObserver to = AmazonAwsUtil.getDownloadObserver(getApplicationContext(),
                    prefix + fileName, // eg lawyers/lawyers.csv // directory+ fileName
                    file);

            to.setTransferListener(new TransferListener() {
                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (state == TransferState.COMPLETED) {
                        Log.d(TAG, "Completed file download");

                        // This file has replaced the old file successfully
                        PreferenceManager.getDefaultSharedPreferences(AmazonJobService.this)
                                .edit()
                                .putInt(preferenceKey + VERSION_CODE, version)
                                .putBoolean(getString(R.string.last_content_update_successful_key), true)
                                .apply();

                    } else if (state == TransferState.FAILED) {
                        onError(id, new TransferException("Transfer failed"));

                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                }

                @Override
                public void onError(int id, Exception ex) {
                    Log.e(TAG, ex.getMessage());
                    file.delete();
                    ((BaseApplication) getApplication()).getDefaultTracker()
                            .send(new HitBuilders.EventBuilder()
                                    .setAction(ACTION_ERROR)
                                    .setCategory(CATEGORY_ERROR)
                                    .setLabel("Download error " + ex.getMessage())
                                    .build());
                }
            });
        }
    }

    /*
     * Check version with file specified.
     */
    private void checkFileVersion(final String directory, final String preferenceKey, final String fileName) {
        Log.d(TAG, "Check file version for " + directory + fileName);
        final File tempPath = new File(getFilesDir(), directory.replaceAll("/", ""));
        if (!tempPath.exists()) {
            tempPath.mkdirs();
        }

        final File file = new File(tempPath, "versionCode.tmp");
        if (!file.exists()) {
            Log.d(TAG, "file did not exist-creating...");
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage());
            }
        }

        final TransferObserver observer = AmazonAwsUtil
                .getDownloadObserver(getApplicationContext(),
                        directory + "versionCode.tmp", // whatever it is named on s3
                        file);

        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    Log.d(TAG, "Completed version file download");

                    SharedPreferences preferences = PreferenceManager
                            .getDefaultSharedPreferences(AmazonJobService.this);

                    // We successfully checked for updates, update this date
                    preferences.edit()
                            .putString(getString(R.string.last_content_check_date),
                                    new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.US).format(new Date()))
                            .apply();

                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new FileReader(file));
                        String line = reader.readLine().trim();
                        int version = Integer.parseInt(line);

                        if (version > preferences
                                .getInt(preferenceKey + VERSION_CODE, -1)) {


                            // We need to update the file--indicate in shared prefs
                            // so that update is resumed on next run of app in case it fails this time
                            preferences.edit()
                                    .putBoolean(getString(R.string.last_content_update_successful_key), false)
                                    .apply();

                            // Newer file is available; download it
                            downloadContent(directory, preferenceKey, fileName, version);
                        }
                        file.delete();
                    } catch (IOException e) {
                        onError(id, e);
                    } finally {
                        try {
                            if (reader != null) {
                                reader.close();
                            }
                        } catch (IOException ex) {
                            onError(id, ex);
                        }
                    }

                } else if (state == TransferState.FAILED) {
                    onError(id, new TransferException("Transfer failed"));
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

            }

            @Override
            public void onError(int id, Exception ex) {
                ex.printStackTrace();
                Log.e(TAG, "onTransferError: checkFileVersion");
                file.delete();
                ((BaseApplication) getApplication()).getDefaultTracker()
                        .send(new HitBuilders.EventBuilder()
                                .setAction(ACTION_ERROR)
                                .setCategory(CATEGORY_ERROR)
                                .setLabel("onTransferError (checkfileVersion): " + ex.getMessage())
                                .build());
            }
        });
    }

    /*
     * Check version, using default bucket as location to search for version file.
     */
    private void checkAppVersion() {
        final File tempPath = getFilesDir();
        if (!tempPath.exists()) {
            tempPath.mkdirs();
        }
        final File file = new File(tempPath, "version.tmp");
        final TransferObserver observer = AmazonAwsUtil
                .getDownloadObserver(getApplicationContext(),
                        "versionCode.tmp", file);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new FileReader(file));
                        String line = reader.readLine().trim();
                        int version = Integer.parseInt(line);
                        PackageInfo packageInfo = getPackageManager()
                                .getPackageInfo(getPackageName(), 0);
                        if (packageInfo != null
                                && version > packageInfo.versionCode) {
                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                    .edit()
                                    .putBoolean(APP_NEEDS_UPDATE_KEY, true)
                                    .apply();
                            Log.i("UpdateTask", "Newer version of app detected");
                        }
                    } catch (IOException | PackageManager.NameNotFoundException e) {
                        onError(id, e);
                    } finally {
                        file.delete();
                        try {
                            if (reader != null) {
                                reader.close();
                            }
                        } catch (IOException ex) {
                            onError(id, ex);
                        }
                    }
                } else if (state == TransferState.FAILED) {
                    onError(id, new TransferException("Transfer Failed"));
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, "onTransferError: checkAppVersion");
                ex.printStackTrace();
                file.delete();
                ((BaseApplication) getApplication()).getDefaultTracker()
                        .send(new HitBuilders.EventBuilder()
                                .setAction(ACTION_ERROR)
                                .setCategory(CATEGORY_ERROR)
                                .setLabel("onTransferError (checkAppVersion): " + ex.getMessage())
                                .build());
            }
        });
    }

    private void updateApp() {
        final File path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
        final File file = new File(path, getApkName());
        final TransferObserver observer = AmazonAwsUtil
                .getDownloadObserver(getApplicationContext(),
                        Constants.APK_FILE_NAME, file);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {

                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                            .edit()
                            .putBoolean(APP_NEEDS_UPDATE_KEY, false)
                            .apply();

                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                    installIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            getApplicationContext(), 0, installIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    NotificationManager mNotifyManager
                            = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
                    mBuilder.setContentTitle(getString(R.string.download_success))
                            .setContentText(getString(R.string.install_update))
                            .setSmallIcon(R.drawable.hafez_logo)
                            .setContentIntent(pendingIntent)
                            .setOngoing(true)
                            .setAutoCancel(false);

                    mNotifyManager.notify(NOTIFICATION_ID_APP_UPDATE, mBuilder.build());
                    sendBroadcast(new Intent(BROADCAST_APP_UPDATE_SUCCESS));
                } else if (state == TransferState.FAILED) {
                    Log.e(TAG, "State is " + state.toString());
                    sendBroadcast(new Intent(BROADCAST_APP_UPDATE_FAILURE));
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d(TAG, "OnTransferError: apk");
                file.delete();
                ((BaseApplication) getApplication()).getDefaultTracker()
                        .send(new HitBuilders.EventBuilder()
                                .setAction(ACTION_ERROR)
                                .setCategory(CATEGORY_ERROR)
                                .setLabel("Transfer error: apk " + ex.getMessage())
                                .build());
            }
        });
    }

}
