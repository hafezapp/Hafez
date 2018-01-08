package org.hrana.hafez.presenter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.hrana.hafez.Constants;
import org.hrana.hafez.R;
import org.hrana.hafez.exception.InvalidClientIdException;
import org.hrana.hafez.exception.OversizeFileException;
import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.Encryptable;
import org.hrana.hafez.model.EncryptedAttachment;
import org.hrana.hafez.model.EncryptedReport;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.net.IApiEndpoint;
import org.hrana.hafez.net.ProgressRequestBody;
import org.hrana.hafez.net.S3MetadataAdapter;
import org.hrana.hafez.presenter.contract.ICryptoPresenter;
import org.hrana.hafez.presenter.contract.IMediaPresenter;
import org.hrana.hafez.presenter.contract.IReportSubmissionPresenter;
import org.hrana.hafez.presenter.contract.IViewContract;
import org.hrana.hafez.util.AmazonAwsUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;

import javax.crypto.SecretKey;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.hrana.hafez.Constants.AWS_OBJECT_STRING;
import static org.hrana.hafez.Constants.CAN_ATTEMPT_RETRY;
import static org.hrana.hafez.Constants.CHARSET;
import static org.hrana.hafez.Constants.HTTP_BAD_REQUEST;
import static org.hrana.hafez.Constants.HTTP_DISALLOW;
import static org.hrana.hafez.Constants.HTTP_FORBIDDEN;
import static org.hrana.hafez.Constants.HTTP_INTERNAL_ERR;
import static org.hrana.hafez.Constants.HTTP_TOO_LARGE;
import static org.hrana.hafez.Constants.HTTP_UNAUTHORIZED;
import static org.hrana.hafez.Constants.INTENT_ACTION_ENCRYPTION_FINISHED;
import static org.hrana.hafez.Constants.INTENT_ACTION_SUBMISSION_STATUS;
import static org.hrana.hafez.Constants.INTENT_ACTION_UPDATE_PROGRESS;
import static org.hrana.hafez.Constants.IS_SUBMISSION_SUCCESS;
import static org.hrana.hafez.Constants.MIDDLEWARE_RESPONSE_CREATED;
import static org.hrana.hafez.Constants.STATUS_MESSAGE;
import static org.hrana.hafez.Constants.SUBMIT_ATTACHMENT_KEY;
import static org.hrana.hafez.Constants.UPLOAD_PROGRESS;

/**
 * Submit reports and attachments. Reports can be submitted either to the server/middleware,
 * or fall back to submissions on AWS/S3 if initial attempt(s) to submit are unsuccessful.
 */
public class ReportSubmissionPresenter implements IReportSubmissionPresenter, Observer<Object> {
    private static final String TAG = "ReportSubmPresenter";
    private IApiEndpoint apiService;
    private Subscription subscription;
    private boolean shouldUseS3 = true, isRetry;
    private Context appContext;
    private TransferListener listener;
    int count = 0;
    long total = 0;

    private ICryptoPresenter cryptoPresenter;
    private IMediaPresenter mediaPresenter;

    private static final int NOTIFY_ID = 23499634;

    public ReportSubmissionPresenter(@NonNull Context appContext,
                                     @NonNull IApiEndpoint apiService,
                                     @NonNull ICryptoPresenter cryptoPresenter,
                                     @NonNull IMediaPresenter mediaPresenter) {
        this.apiService = apiService;
        this.appContext = appContext.getApplicationContext();
        this.cryptoPresenter = cryptoPresenter;
        this.mediaPresenter = mediaPresenter;
    }

    /*
     * Submit reports to appropriate endpoint.
     * Reports can either be submitted to server endpoint directly, or
     * in the case of the backup method, to AWS.
     *
     * @param   EncryptedReport          {@link EncryptedReport} object to send
     * @return  Observable<String>  Observable to monitor report.
     *
     */
    @Override
    public Observable<String> submit(final EncryptedReport encryptedReport) {
        Observable<String> reportObs;
        if (shouldUseS3) { // Submitting to AWS, put report in a bucket
            AmazonS3Client s3Client = AmazonAwsUtil.getS3Client(appContext);

            final String filename = Long.toHexString(new Random().nextLong()); //@Todo @Improvement new filename doesn't matter, as long as we avoid collision. Less expensive way?
            final byte[] reportBytes = reportToBytes(encryptedReport);

            PutObjectResult response = s3Client.putObject(new PutObjectRequest
                    (Constants.SUBMISSION_BUCKET, // Bucket
                            Constants.SUBMIT_REPORT_KEY + filename, // 'Subdirectory' and filename
                            new ByteArrayInputStream(reportBytes), // report as inputstream
                            generateMetaData(reportBytes.length)));
            reportObs = Observable.just(response.toString());

        } else { // Submitting to middleware via endpoint
            reportObs = apiService.postEncryptedReport(
                    encryptedReport.getClient_version(),
                    encryptedReport.getSubmission_time(),
                    encryptedReport.getEncryption_key_id(),
                    encryptedReport.getSecurity_token(),
                    encryptedReport.getEncrypted_blob());
        }
        return reportObs
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "apiService error");
                    }
                });
    }

    /*
     * Process attachment submissions by returning an {@link Observable<String>} that will trigger submission when
     * it is subscribed to.
     *
     * If backup service is selected, triggers submission to S3 bucket.
     * If backup service is not selected, triggers submission to middleware.
     *
     * @param   List<Attachment>    {@link Attachment} objects which contain reference to encrypted File and encrypted private details.
     * @return  Observable<String>  Observable to emit submission progress.
     */
    @Override
    public Observable<String> submit(final EncryptedAttachment attachment) {
        final Observable<String> attachmentObs;
        if (shouldUseS3) { // Submitting to AWS, put report in a bucket
            final String filename = attachment.getFile().getName();
            final AmazonS3Client s3Client = AmazonAwsUtil.getS3Client(appContext);

            attachmentObs = Observable.create(new Observable.OnSubscribe<TransferState>() {
                @Override
                public void call(final Subscriber<? super TransferState> subscriber) {

                    listener = new TransferListener() {
                        private int recentProgress = 0;

                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            Log.d(TAG, "State " + state);

                            if (state == TransferState.COMPLETED) {
                                Log.d(TAG, "Attachment upload completed.");
                                subscriber.onNext(state); // upload the metadata
                                subscriber.onCompleted();
                            } else if (state == TransferState.FAILED) {
                                subscriber.onError(new Throwable("Failed transfer"));
                            } else if (state == TransferState.CANCELED) {
                                subscriber.onError(new Throwable("Cancelled transfer"));
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            float newProgressFloat = ((float) bytesCurrent / (float) bytesTotal);
                            int newProgress = (int) (newProgressFloat * 100);
                            Log.d(TAG, "Progress changed: ....... (" + bytesCurrent + "/" + bytesTotal + "): " + newProgress + "%");
                            if (newProgress - recentProgress >= 5) { // don't publish all the time
                                recentProgress = newProgress;
                                publishProgress(recentProgress);
                            }
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            subscriber.onError(ex);
                        }
                    };

                    final TransferObserver uploadObserver = AmazonAwsUtil.getUploadObserver(
                            appContext,
                            SUBMIT_ATTACHMENT_KEY + attachment.getFile().getName(),
                            attachment.getFile());

                    uploadObserver.setTransferListener(listener);
                }
            })
                    .observeOn(Schedulers.io())
                    .filter(new Func1<TransferState, Boolean>() {
                        @Override
                        public Boolean call(TransferState transferState) {
                            Log.d(TAG, "State " + transferState.toString());
                            return transferState == TransferState.COMPLETED;
                        }
                    })
                    .concatMap(new Func1<TransferState, Observable<? extends String>>() {
                        @Override
                        public Observable<? extends String> call(TransferState transferState) {
                            Log.d(TAG, "Sending metadata...");

                            // Then upload metadata
                            byte[] metadata = metadataToBytes(attachment);

                            // returns nonnull result (success) or throws.
                            PutObjectResult result = s3Client.putObject(new PutObjectRequest(
                                    Constants.SUBMISSION_BUCKET,
                                    Constants.SUBMIT_MEDATADA_KEY + filename,
                                    new ByteArrayInputStream(metadata),
                                    generateMetaData(metadata.length)));

                            return Observable.just(result.toString());
                        }
                    });

        } else { // Submitting to middleware via endpoint; publish file upload progress
            attachmentObs = apiService.postEncryptedAttachment(
                    RequestBody.create(MediaType.parse("text/plain"), attachment.getClientVersion()),
                    RequestBody.create(MediaType.parse("text/plain"), attachment.getTimestamp()),
                    RequestBody.create(MediaType.parse("text/plain"), attachment.getEncryptionId()),
                    RequestBody.create(MediaType.parse("text/plain"), attachment.getSecurityToken()),
                    RequestBody.create(MediaType.parse("application/octet-stream"), attachment.getPrivateDetails()),
                    ProgressRequestBody.create(attachment.getFile(), new IViewContract.UploadListener() {
                        int latestProgress = 0;

                        @Override
                        public void update(int newProgress) {
                            if (newProgress - latestProgress >= 5) {
                                latestProgress = newProgress;
                                publishProgress(latestProgress);
                            }
                        }
                    }));
        }
        return attachmentObs
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        count++; // number of files uploaded
                        boolean res = attachment.getFile().delete();
                        Log.d(TAG, "Delete attachment file: " + res);
                    }
                });
    }

    /*
     * Metadata for objects in S3 bucket. Metadata is autogenerated for files, but
     * required for non file types.
     *
     * All objects are encoded with default charset CHARSET, and application/json content type.
     *
     * @param   length      number of bytes in file/array
     * @returns ObjectMetadata
     */
    private ObjectMetadata generateMetaData(long length) {
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentLength(length);
        metaData.setContentType("application/json");
        metaData.setContentEncoding(CHARSET);

        return metaData;
    }

    /*
     * Convert report into a JSON object and return this object in a byte[]. This
     * avoids the creation of temporary report files, but should only be used for small
     * files such as text-based reports. Larger files should not be kept in memory.
     *
     */
    private byte[] reportToBytes(EncryptedReport encryptedReport) {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<EncryptedReport> adapter = moshi.adapter(EncryptedReport.class);
        String mfile = adapter.toJson(encryptedReport);
        return mfile.getBytes(Charset.forName(CHARSET));
    }

    /*
     * Convert metadata into a JSON object and return this object in a byte[].
     * This JSON contains the unencrypted metadata information (client version, etc) as
     * well as the encrypted information.
     */
    private byte[] metadataToBytes(EncryptedAttachment attachment) {
        Moshi moshi = new Moshi.Builder().add(new S3MetadataAdapter()).build();
        JsonAdapter<EncryptedAttachment> adapter = moshi.adapter(EncryptedAttachment.class);

        return adapter.toJson(attachment).getBytes(Charset.forName(CHARSET));
    }

    /*
     * Set whether to use backup submission method, such as
     * Amazon Web Services, instead of direct submission.
     */
    @Override
    public void setUseBackupAttempt(boolean shouldUseBackup) {
        this.shouldUseS3 = shouldUseBackup;
    }

    @Override
    public void sendSubmission(final Report report, final List<Uri> uris) {
        total = uris.size(); // Relevant for progress

        final Observable<Encryptable> encrypted = encryptSubmission(report, uris);
        final Observable<String> sent = sendEncryptedItems(encrypted);

        // subscribe
        launchSubscription(sent);
    }

    public Observable<String> sendEncryptedItems(Observable<Encryptable> intermedObs) {
        return intermedObs
                .observeOn(Schedulers.io()) // Networking on IO
                .concatMap(new Func1<Encryptable, Observable<? extends String>>() {
                    @Override
                    public Observable<? extends String> call(Encryptable encryptable) {
                        if (encryptable instanceof EncryptedAttachment) {  // submit attachment
                            return submit((EncryptedAttachment) encryptable);
                        } else if (encryptable instanceof EncryptedReport) { // submit report
                            return submit((EncryptedReport) encryptable);
                        } else {  // must always be one of the types above; fail
                            return Observable.error(new IllegalArgumentException("Unknown Encryptable type"));
                        }
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "error in sendEncryptedItems" + throwable.getMessage());
                        if (throwable instanceof SocketTimeoutException) {
                            Log.e(TAG, "SocketTimeoutException saw "
                                    + ((SocketTimeoutException) throwable).bytesTransferred
                                    + " bytes transferred ");
                            onError(throwable);
                        } else if (throwable instanceof TypeNotPresentException) {
                            Log.e(TAG, "MimeType not present");
                            onError(throwable);
                        }
                    }
                }).filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        Log.d(TAG, "String filter is " + s);
                        return s.contains(MIDDLEWARE_RESPONSE_CREATED)
                                || s.contains(AWS_OBJECT_STRING);
                    }
                }).doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        Log.d(TAG, "sendEncryptedItems is Completed");
                    }
                }).doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        Log.d(TAG, "OnTerminate");
                        count = 0;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()); // only observe success
    }

    public void launchSubscription(Observable<String> stringObservable) {
        subscription = stringObservable
                .subscribeOn(Schedulers.io())
                .subscribe(this);
    }

    /*
     * Emit observable of encrypted items for submission.
     */
    public Observable<Encryptable> encryptSubmission(final Report report, final List<Uri> uris) {

        // encrypt report
        final Observable<EncryptedReport> encryptedReportObservable = buildEncryptedReport(report); //@todo to use

        try {
            final SecretKey key = cryptoPresenter.generateAttachmentKey(); // Same encryption_key for all attachments
            final List<Attachment> attachments = mediaPresenter.toAttachments(report, uris, key);

            // Encrypt attachments
            final Observable<EncryptedAttachment> encryptedAttachments = buildEncryptedAttachments(attachments, key);

            return Observable.concat(
                    encryptedReportObservable,
                    (report.isWithAttachments() ?
                            encryptedAttachments
                            : Observable.<Encryptable>empty()))
                    .doOnCompleted(new Action0() {
                        @Override
                        public void call() {
                            Intent encryptionFinishedIntent = new Intent(INTENT_ACTION_ENCRYPTION_FINISHED);
                            LocalBroadcastManager.getInstance(appContext).sendBroadcast(encryptionFinishedIntent);
                        }
                    });

        } catch (InvalidClientIdException | NoSuchAlgorithmException ex) {
            onError(ex);
            Log.e(TAG, ex.getMessage());
        }

        return null;
    }

    @Override
    public void cancelSubmission() {
        if (subscription != null && !subscription.isUnsubscribed()) {
            Log.d(TAG, "Unsubscribing...");
            subscription.unsubscribe();
        } else {
            Log.e(TAG, "Could not unsubscribe--null subscription or already unsubscribed");
        }
    }

    /*
         * Returns true if this is a potetially recoverable error and client should
         * try submitting again with a backup method (in this case, AWS S3), false otherwise.
         *
         */
    private boolean shouldRetrySubmissionWithBackup(Throwable e) {
        int code;
        return (!(e instanceof OversizeFileException) &&
                !(e instanceof InvalidClientIdException) &&
                !(e instanceof TypeNotPresentException) &&
                (!(e instanceof HttpException) ||
                        ((code = ((HttpException) e).code()) == HTTP_INTERNAL_ERR ||
                                code == HTTP_DISALLOW ||
                                code == HTTP_FORBIDDEN)));
    }

    /*
     * Build observable that emits encrypted reports.
     */
    private Observable<EncryptedReport> buildEncryptedReport(final Report report) {
        return Observable.just(report)
                .subscribeOn(Schedulers.computation()) // Encryption on computation thread
                .flatMap(new Func1<Report, Observable<EncryptedReport>>() {
                    @Override
                    public Observable<EncryptedReport> call(Report report) {
                        return Observable.just(cryptoPresenter.encryptReport(report));
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "View: Report encryption error");
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()); // Observe success (or not) on main
    }

    /*
     * Build observable that emits encrypted attachments
     */
    private Observable<EncryptedAttachment> buildEncryptedAttachments(final List<Attachment> attachments, final SecretKey key) {
        return Observable.from(attachments)
                .subscribeOn(Schedulers.computation()) // Encryption on computation thread
                .concatMap(new Func1<Attachment, Observable<EncryptedAttachment>>() {
                    @Override
                    public Observable<EncryptedAttachment> call(final Attachment attachment) {
                        try {
                            final String tempName = attachment.getAttachmentId();
                            File f = new File(appContext.getFilesDir(), tempName);
                            InputStream is = mediaPresenter.openFile(attachment);
                            return Observable.just(cryptoPresenter.encryptAttachment
                                    (f, key, attachment, is));
                        } catch (FileNotFoundException ex) {
                            Log.e(TAG, "File not found exception in buildSubmissionRequest");
                        }
                        return Observable.empty(); // emit nothing, keep going normally
                    }
                });
    }

    /*
     * Subscriber<String> methods
     */
    @Override
    public void onCompleted() {
        // Update UI
        Log.d(TAG, "OnCompleted");
        count = 0;

        Intent successIntent = new Intent(INTENT_ACTION_SUBMISSION_STATUS);
        successIntent.putExtra(IS_SUBMISSION_SUCCESS, true);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(successIntent);
    }

    @Override
    public void onError(Throwable e) {
        count = 0;

        boolean canAttemptRetry = false;
        String message = null;

        if (e instanceof SocketTimeoutException) {
            canAttemptRetry = true;
        } else if (e instanceof OversizeFileException) {
            message = appContext.getString(R.string.error_oversize_submission_message);
        } else if (e instanceof InvalidClientIdException) {
            message = appContext.getString(R.string.error_client_id_message);
        } else if (e instanceof TypeNotPresentException) {
            message = appContext.getString(R.string.file_type_not_determined);
        } else if (e instanceof HttpException) {
            int responseCode = ((HttpException) e).code();
            if (responseCode == HTTP_BAD_REQUEST) { // 400
                message = appContext.getString(R.string.error_submission);
                canAttemptRetry = true;

            } else if (responseCode == HTTP_UNAUTHORIZED) { // 401
                Log.e(TAG, "onError: Unauthorized");
                message = appContext.getString(R.string.error_max_submissions_message);

            } else if (responseCode == HTTP_TOO_LARGE) { // 413
                Log.e(TAG, "onError: Entity too large");
                message = appContext.getString(R.string.error_oversize_submission_message);

            } else if (responseCode == HTTP_INTERNAL_ERR) { // 500
                Log.e(TAG, "onError: Internal server error");
                message = appContext.getString(R.string.server_error_submission);
                canAttemptRetry = true;
            }
        }

        Log.e(TAG, "Failure: " + e.getMessage());
        Intent failIntent = new Intent(INTENT_ACTION_SUBMISSION_STATUS);
        failIntent.putExtra(IS_SUBMISSION_SUCCESS, false);
        if (message != null) {
            failIntent.putExtra(STATUS_MESSAGE, message);
        }
        failIntent.putExtra(CAN_ATTEMPT_RETRY, canAttemptRetry);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(failIntent);
    }

    @Override
    public void onNext(Object o) {
    }

    /*
     * Update notification with total progress.
     * 'Total progress' means the percentage of bytes uploaded in reference to all file bytes.
     *
     * @param   int currentPercent  Current upload progress expressed as a percentage.
     */
    public void publishProgress(int currentPercent) {
        int overallProgress;
        if (total > 1) {

            // nth attachment progress offset; 100% through attachment 2/3 is approx 66% finished overall
            int percentOffset = (int) (count * 100 / total);

            // percent overall progress
            overallProgress = (int) (currentPercent / total) + percentOffset;
        } else {
            overallProgress = currentPercent;
        }

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(INTENT_ACTION_UPDATE_PROGRESS);
        broadcastIntent.putExtra(UPLOAD_PROGRESS, overallProgress);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(broadcastIntent);
    }

    @Override
    public void setTotalBytes(long total) {
        this.total = total;
    }
}
