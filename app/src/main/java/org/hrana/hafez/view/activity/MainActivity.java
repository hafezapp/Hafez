package org.hrana.hafez.view.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;

import org.hrana.hafez.BuildConfig;
import org.hrana.hafez.Constants;
import org.hrana.hafez.R;
import org.hrana.hafez.di.BaseApplication;
import org.hrana.hafez.di.component.DaggerIPresenterComponent;
import org.hrana.hafez.di.module.PresenterModule;
import org.hrana.hafez.enums.CustomEnum;
import org.hrana.hafez.presenter.ApiConstants;
import org.hrana.hafez.presenter.contract.IFeedPresenter;
import org.hrana.hafez.presenter.contract.IViewContract;
import org.hrana.hafez.service.register.RegistrationJobIntentService;
import org.hrana.hafez.service.update.AmazonJobService;
import org.hrana.hafez.util.MediaUtil;
import org.hrana.hafez.view.fragment.LegalCaseFragment;
import org.hrana.hafez.view.fragment.LegalContactFragment;
import org.hrana.hafez.view.fragment.NewsFragment;
import org.hrana.hafez.view.fragment.ReportSubmissionFragment;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.hrana.hafez.Constants.ACTION_CLICK;
import static org.hrana.hafez.Constants.CATEGORY_BUTTON_PRESS;
import static org.hrana.hafez.Constants.CATEGORY_MEDIA;
import static org.hrana.hafez.Constants.CATEGORY_UPDATE;
import static org.hrana.hafez.Constants.CLIENT_TOKEN_KEY;
import static org.hrana.hafez.Constants.INTENT_ACTION_CHECK_UPDATE_CONTENT;
import static org.hrana.hafez.Constants.NOTIFICATION_ID_APP_UPDATE;
import static org.hrana.hafez.Constants.PERMISSION_IMPORT_FILES_CODE;
import static org.hrana.hafez.Constants.PERMISSION_WRITE_STORAGE_CODE;
import static org.hrana.hafez.Constants.SIMPLE_DATE_FORMAT;

/**
 * Main Activity class. Launch the app, check whether onboarding or updates are necessary,
 * and host the four main app views ({@link NewsFragment}, {@link LegalCaseFragment},
 * {@link LegalContactFragment}
 */
public class MainActivity extends BaseActivity implements IViewContract.MainMediaView {
    private static final String TAG = "MainActivity";
    private static final String CURRENT_MEDIA_PATH = "CurrentMediaPath"; // for savedInstanceState
    private static final int COUNT = 4;
    private String mCurrentMediaPath = null;
    private CustomEnum.MEDIA_TYPE currentMediaAction;
    private boolean isReportingEnabled = true; // only disabled during active sending of existing report
    private BroadcastReceiver updateReceiver;
    private IViewContract.ReviewMediaView mediaReportView;
    private static final int POSITION_CASES = 0, POSITION_LAWYERS = 1, POSITION_NEWS = 2, POSITION_REPORT = 3;

    @Inject
    SharedPreferences preferences;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    @BindView(R.id.container)
    ViewPager mViewPager;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.button_container)
    LinearLayout quickButtonContainer;
    @BindView(R.id.button_submit)
    FloatingActionButton submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // DI
        DaggerIPresenterComponent.builder()
                .iApplicationComponent(BaseApplication.get(this).getComponent())
                .presenterModule(new PresenterModule())
                .build()
                .inject(this);

        // Check if onboarding has happened; if not, launch onboarding
        if (!preferences.getBoolean(getString(R.string.onboarding_complete_key), false)) {
            preferences.edit()
                    .putBoolean(getString(R.string.onboarding_complete_homepage_key), false)
                    .apply();

            Intent intent = new Intent(this, IntroductionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return; // We're done, no point in continuing to create this activity.
        }

        // Check if EUA has been accepted. This is a requirement for using the app
        if (!preferences.getBoolean(getString(R.string.eua_accepted_key), false)) {
            Intent euaIntent = new Intent(this, EuaActivity.class);
            startActivity(euaIntent);
            finish();
            return; // We're done
        }

        // Check if app has registered GCM token.
        if (preferences.getString(CLIENT_TOKEN_KEY, "").isEmpty()) {
            Intent refreshIntent = new Intent();
            RegistrationJobIntentService.enqueueWork(this, refreshIntent);
        }

        // UI
        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor
                    (ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }
        setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        // Register for app update status broadcasts
        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_APP_UPDATE_FAILURE)) {
                    Toast.makeText(MainActivity.this,
                            context.getString(R.string.error_update_failed),
                            Toast.LENGTH_LONG).show();
                } else if (intent.getAction().equals(Constants.BROADCAST_APP_UPDATE_SUCCESS)) {
                    Toast.makeText(MainActivity.this,
                            getString(R.string.download_success),
                            Toast.LENGTH_LONG).show();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.BROADCAST_APP_UPDATE_FAILURE);
        filter.addAction(Constants.BROADCAST_APP_UPDATE_SUCCESS);

        registerReceiver(updateReceiver, filter);

        // Check if activity being recreated
        if (savedInstanceState != null) {
            mCurrentMediaPath = savedInstanceState.getString(CURRENT_MEDIA_PATH);
        }

        // Check if app needs update
        if (preferences.getBoolean(Constants.APP_NEEDS_UPDATE_KEY, false)) {
            requestAppUpdate();
        }

        // Check if version was just updated. If so, cancel update notification
        int versionCode = preferences.getInt(Constants.VERSION_CODE, -1);
        if (versionCode == -1 || BuildConfig.VERSION_CODE > versionCode) { // a new install
            preferences.edit()
                    .putInt(Constants.VERSION_CODE, BuildConfig.VERSION_CODE)
                    .apply();
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .cancel(NOTIFICATION_ID_APP_UPDATE);
        }

        // Check if content is up to date
        launchContentUpdateService();

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        final SectionsPagerAdapter mSectionsPagerAdapter
                = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(3); // No need to destroy fragments since there are only 4.
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            private String getScreenName(int position) {
                String name;
                switch (position) {
                    case POSITION_CASES:
                        return LegalCaseFragment.class.getSimpleName();
                    case POSITION_LAWYERS:
                        return LegalContactFragment.class.getSimpleName();
                    case POSITION_NEWS:
                        return NewsFragment.class.getSimpleName();
                    case POSITION_REPORT:
                        return ReportSubmissionFragment.class.getSimpleName();
                    default:
                        name = "<Unspecified>";
                }
                return name;
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                // GA
                setAnalyticsScreenName(getScreenName(position));

                if (position == POSITION_REPORT && isReportingEnabled
                        && quickButtonContainer.getVisibility() != View.VISIBLE) {

                    showViewWithAnimation(quickButtonContainer);
                    showViewWithAnimation(submitButton);
                } else if (position != POSITION_REPORT && quickButtonContainer.getVisibility() == View.VISIBLE) {
                    hideViewWithAnimation(quickButtonContainer);
                    hideViewWithAnimation(submitButton);
                }

                if (position == POSITION_NEWS) {
                        IFeedPresenter presenter = ((NewsFragment) mSectionsPagerAdapter.getItem(position)).getFeedPresenter();
                        if (presenter != null && !presenter.hasRecentCache()) {
                            ((NewsFragment) mSectionsPagerAdapter.getItem(position)).loadFeed(ApiConstants.FEED_URL);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabLayout.setupWithViewPager(mViewPager);

        // set icons
        final TypedArray tabIcons = getResources().obtainTypedArray(R.array.tab_icons);
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            tabLayout.getTabAt(i).setIcon(tabIcons.getDrawable(i));
        }
        tabIcons.recycle();

        mViewPager.setCurrentItem(mSectionsPagerAdapter.getCount() - 1); // Start at rightmost item.

        // See if need to show tutorial or not
        if (!preferences.getBoolean(getString(R.string.onboarding_complete_homepage_key), false)) {
            launchTutorial();
            preferences.edit()
                    .putBoolean(getString(R.string.onboarding_complete_homepage_key), true)
                    .apply();
        }
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() == 3 &&
                (mediaReportView != null && mediaReportView.isInProgress())) {
            mediaReportView.clearProgressWithWarning();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void registerChild(IViewContract.ReviewMediaView child) {
        this.mediaReportView = child;
    }

    @Override
    public void unregisterChild() {
        this.mediaReportView = null;
    }

    void launchTutorial() {

        // GA
        sendAnalyticsHitEvent(Constants.ACTION_VIEW, CATEGORY_BUTTON_PRESS, "Tutorial");

        final Rect fullScreenRect = new Rect(), tabRect = new Rect(), quickButtonRect = new Rect(), testRect = new Rect();
        getWindowManager().getDefaultDisplay().getRectSize(fullScreenRect);
        tabLayout.getGlobalVisibleRect(tabRect);
        tabLayout.getLocalVisibleRect(testRect);
        quickButtonContainer.getGlobalVisibleRect(quickButtonRect);
        final TapTargetSequence tutorialSequence = new TapTargetSequence(this)
                .targets(
                        TapTarget.forView(submitButton, getString(R.string.tutorial_1_title), getString(R.string.tutorial_1_message))
                                .cancelable(false)
                                .targetRadius(50)
                                .titleTextSize(28)
                                .drawShadow(true)
                                .descriptionTextSize(22)
                                .dimColor(R.color.black)
                                .transparentTarget(true)
                                .id(0),
                        TapTarget.forBounds(new Rect(fullScreenRect.left, fullScreenRect.bottom, fullScreenRect.right, fullScreenRect.bottom), getString(R.string.tutorial_2_title), getString(R.string.tutorial_2_message))
                                .cancelable(false)
                                .drawShadow(true)
                                .targetRadius(64)
                                .titleTextSize(28)
                                .descriptionTextSize(22)
                                .dimColor(R.color.black)
                                .transparentTarget(true)
                                .id(1),
                        TapTarget.forView(tabLayout, getString(R.string.tutorial_3_title), getString(R.string.tutorial_3_message))
                                .cancelable(false)
                                .targetRadius(20)
                                .targetCircleColor(R.color.white)
                                .outerCircleColor(R.color.colorPrimary)
                                .titleTextSize(28)
                                .drawShadow(true)
                                .textColor(R.color.white)
                                .descriptionTextSize(22)
                                .transparentTarget(true)
                                .id(2),
                        TapTarget.forToolbarOverflow(toolbar, getString(R.string.tutorial_4_title), getString(R.string.tutorial_4_message))
                                .cancelable(false)
                                .titleTextSize(28)
                                .descriptionTextSize(22)
                                .dimColor(R.color.black)
                                .targetCircleColor(R.color.colorAccent)
                                .id(3),
                        TapTarget.forBounds(fullScreenRect, getString(R.string.tutorial_5_title), getString(R.string.tutorial_5_message))
                                .targetRadius(0)
                                .titleTextSize(30)
                                .descriptionTextSize(22)
                                .dimColor(R.color.black)
                                .cancelable(true)
                                .id(4));
        tutorialSequence.considerOuterCircleCanceled(true);
        tutorialSequence.continueOnCancel(true);
        tutorialSequence.start();
    }

    private void showViewWithAnimation(final View view) {
        view.setAlpha(0.0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1.0f)
                .setDuration(200)
                .start();
    }

    private void hideViewWithAnimation(final View view) {
        view
                .animate()
                .alpha(0.0f)
                .setDuration(200)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        view.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    /*
     * Check if static content is up to date, and if not, downoad and update new content.
     * Launches a wakeful service to check content files and (if available) download current version.
     * Check for new content at most once a week to avoid unnecessary data and downloads.
     */
    private void launchContentUpdateService() {
        boolean needsUpdate = true; // default is to check for updates
        SimpleDateFormat sdf = new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.US);
        try {
            Date lastCheckedDate = sdf.parse(preferences.getString(getString(R.string.last_content_check_date), ""));
            if (new Date().getTime() - lastCheckedDate.getTime() <= Constants.CHECK_DATE_INTERVAL
                    && preferences.getBoolean(getString(R.string.last_content_update_successful_key), false)) {
                needsUpdate = false;
            }
        } catch (ParseException ex) {
            Log.e(TAG, "error parsing date from shared preferences--proceed with check");
        }

        if (needsUpdate) {
            Intent intent = new Intent();
            intent.setAction(INTENT_ACTION_CHECK_UPDATE_CONTENT);
            AmazonJobService.enqueueWork(this, intent);
        }
    }

    /*
     * Request download of newer version of the app
     */
    private void sendUpdateIntent() {
        Intent getUpdateIntent = new Intent();
        getUpdateIntent.setAction(Constants.INTENT_ACTION_START_UPDATE);
        AmazonJobService.enqueueWork(this, getUpdateIntent);
    }

    /*
     * Display a dialog asking whether user want to download an app update. If so, launch update download.
     * This only applies to users who sideload the app (users who do not download the app from the Play Store).
     */
    private void requestAppUpdate() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.update_prompt_title))
                .setMessage(getString(R.string.update_prompt))
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // See if user has granted write permissions. If not,
                        // request permissions to download new apk.
                        int permission = ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (permission != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    PERMISSION_WRITE_STORAGE_CODE);
                        } else {
                            sendUpdateIntent();
                        }
                        sendAnalyticsHitEvent(ACTION_CLICK, CATEGORY_UPDATE, "Request App Update");

                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendAnalyticsHitEvent(ACTION_CLICK, CATEGORY_UPDATE, "Dismiss App Update");
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public void allowReporting(boolean isAllowed) {
        isReportingEnabled = isAllowed;
        if (isAllowed && (quickButtonContainer.getVisibility() == View.GONE
                || submitButton.getVisibility() == View.GONE)) {
            showViewWithAnimation(quickButtonContainer);
            showViewWithAnimation(submitButton);
        } else {
            hideViewWithAnimation(quickButtonContainer);
            hideViewWithAnimation(submitButton);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (preferences.getInt(getString(R.string.uninstall_options_value), 0) == Constants.UNINSTALL_ON_MENU) {
            menu.add(0, Constants.ID_UNINSTALL, 0, R.string.one_click_uninstall);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        } else if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (id == Constants.ID_UNINSTALL) {
            Uri packageUri = Uri.parse("package:" + getPackageName());
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
            startActivity(uninstallIntent);
            return true;
        } else if (id == R.id.action_eua) {
            Intent euaIntent = new Intent(this, EuaActivity.class);
            euaIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(euaIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.button_submit)
    public void onClickSubmit() {
        sendAnalyticsHitEvent(ACTION_CLICK, CATEGORY_BUTTON_PRESS, "Submit Report");
        if (mediaReportView != null) {
            mediaReportView.onRequestSubmission();
        } else {
            Log.e(TAG, "Found wrong fragment type");
        }
    }

    @OnClick(R.id.goto_import)
    public void importMedia() {
        sendAnalyticsHitEvent(ACTION_CLICK, CATEGORY_MEDIA, "Import Media");
        int permission = ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_IMPORT_FILES_CODE);
        } else {
            launchImport();
        }
    }

    /*
     * Prompt for importing a new file
     */
    private void launchImport() {
        Intent intent;
        int requestId = Constants.REQUEST_FILE_IMPORT;
        if (Build.VERSION.SDK_INT >= 19) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        // String cardMediaId = mCardModel.getStoryPath().getId() + "::" + mCardModel.getId() + "::" + MEDIA_PATH_KEY;
        // Apply is async and fine for UI thread. commit() is synchronous
        startActivityForResult(intent, requestId); // if handling in Fragment: not getActivity().start...
    }

    @OnClick(R.id.capture_photo)
    public void onCamera() {
        sendAnalyticsHitEvent(ACTION_CLICK, CATEGORY_MEDIA, "Camera");
        checkCapturePermissions(this, (currentMediaAction = CustomEnum.MEDIA_TYPE.IMAGE));
    }

    @OnClick(R.id.capture_video)
    public void onVideo() {
        sendAnalyticsHitEvent(ACTION_CLICK, CATEGORY_MEDIA, "Video");
        checkCapturePermissions(this, (currentMediaAction = CustomEnum.MEDIA_TYPE.VIDEO));
    }

    @OnClick(R.id.capture_audio)
    public void onAudio() {
        sendAnalyticsHitEvent(ACTION_CLICK, CATEGORY_MEDIA, "Audio");
        checkCapturePermissions(this, (currentMediaAction = CustomEnum.MEDIA_TYPE.AUDIO));
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (permsRequestCode == PERMISSION_WRITE_STORAGE_CODE) {
                sendUpdateIntent();
            } else if (permsRequestCode == PERMISSION_IMPORT_FILES_CODE) {
                launchImport();
            } else if (permsRequestCode == 200) { // launch camera, video or audio intent
                captureMedia(currentMediaAction);
            } else { //?
                super.onRequestPermissionsResult(permsRequestCode, permissions, grantResults);
            }
        } else {
            Toast.makeText(this, getString(R.string.error_capture_data_device_permission),
                    Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Check whether need to request runtime permissions to access device storage.
     */

    public void checkCapturePermissions(Activity activity, CustomEnum.MEDIA_TYPE mediaType) {

        // Newer phones request permission at runtime
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                startRequestPermissions();

            } else { // permission was granted
                captureMedia(mediaType);
            }
        } else { // Older build version, don't need to request runtime permissions at all

            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                captureMedia(mediaType);
            } else {
                Toast.makeText(activity,
                        activity.getString(R.string.error_capture_data_device_permission),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*
     * Supply a permission request code and request permission to read and write
     * from device storage.
     * The caller should override onPermissionRequestResult in order
     * to take action after permission request.
     */
    @TargetApi(23)
    public void startRequestPermissions() {
        int permissionRequestCode = 200;
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                permissionRequestCode);
    }

    public void captureMedia(final CustomEnum.MEDIA_TYPE mediaType) {
        Intent intent = null;
        int requestId = -1;
        File mediaFile = null;

        try {
            switch (mediaType) {
                case AUDIO:
                    intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                    requestId = Constants.REQUEST_AUDIO_CAPTURE;
                    mediaFile = MediaUtil.createMediaFile(".mp3",
                            new File(Environment.getExternalStorageDirectory(),
                                    Environment.DIRECTORY_MUSIC),
                            getExternalFilesDir(Environment.DIRECTORY_MUSIC));
                    break;
                case IMAGE:
                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    requestId = Constants.REQUEST_IMAGE_CAPTURE;
                    mediaFile = MediaUtil.createMediaFile(".jpg",
                            new File(Environment.getExternalStorageDirectory(),
                                    Environment.DIRECTORY_PICTURES),
                            getExternalFilesDir(Environment.DIRECTORY_PICTURES));
                    break;

                case VIDEO:
                    intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    requestId = Constants.REQUEST_VIDEO_CAPTURE;
                    mediaFile = MediaUtil.createMediaFile(".mp4",
                            new File(Environment.getExternalStorageDirectory(),
                                    Environment.DIRECTORY_MOVIES),
                            getExternalFilesDir(Environment.DIRECTORY_MOVIES));
                    break;
                default:
                    break;
            }
        } catch (IOException ex) {
            Toast.makeText(this, getString(R.string.capture_error),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (intent != null
                && intent.resolveActivity(getPackageManager()) != null
                && mediaFile != null) {

            setCurrentMediaPath(mediaFile.getAbsolutePath());

            intent.putExtra(MediaStore.EXTRA_OUTPUT,
                    FileProvider.getUriForFile(this,
                            BuildConfig.APPLICATION_ID + ".provider",
                            mediaFile));
            startActivityForResult(intent, requestId);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.error_no_capable_program))
                    .setMessage(getString(R.string.find_program_prompt))
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            promptSearch(mediaType.toString());
                            dialog.dismiss();
                        }
                    })
                    .setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();
        }
    }

    /*
     * Search for applications with the capable feature based on
     * @param String query.
     * If Play Services is installed, launch search in the Google Play app.
     * Otherwise, launch search in a url.
     */
    private void promptSearch(String query) {
        boolean hasGooglePlayMarket = false;
        try {
            String urlSafeQuery = URLEncoder.encode(query, "UTF-8");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=" + urlSafeQuery + "&c=apps"));

            final List<ResolveInfo> applications = getPackageManager()
                    .queryIntentActivities(marketIntent, 0);
            for (ResolveInfo app : applications) {

                // look for Google Play app
                if (app.activityInfo.applicationInfo.packageName
                        .equals("com.android.vending")) {

                    ActivityInfo otherAppActivity = app.activityInfo;
                    ComponentName componentName = new ComponentName(
                            otherAppActivity.applicationInfo.packageName,
                            otherAppActivity.name
                    );

                    marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    // Make sure that only the Google Play app is allowed to
                    // intercept the intent
                    marketIntent.setComponent(componentName);
                    hasGooglePlayMarket = true;
                }
            }

            if (hasGooglePlayMarket) {
                startActivity(marketIntent);
            } else { // Try a web browser
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/search?q=" + urlSafeQuery + "&c=apps"));
                startActivity(intent);
            }
        } catch (UnsupportedEncodingException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case Constants.REQUEST_AUDIO_CAPTURE:
                case Constants.REQUEST_IMAGE_CAPTURE:
                case Constants.REQUEST_VIDEO_CAPTURE:

                    String filepath = getCurrentMediaPath();

                    MediaScannerConnection.scanFile(this, new String[]{filepath}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.d("HomePageParent", "Media scan completed with "
                                            + (uri == null ? "failure" : "success"));
                                }
                            });

                    if (mediaReportView != null) {
                        mediaReportView.addAttachmentFromFilePath(filepath);
                    } else {
                        Log.e(TAG, "MediaView is null");
                    }
                    break;
                case Constants.REQUEST_FILE_IMPORT:
                    Uri uri;
                    if (intent != null
                            && ((uri = intent.getData()) != null)) {
                        try {
                            if (uri.getScheme().equals("content")) {
                                if (mediaReportView != null) {
                                    mediaReportView.addAttachment(uri);
                                } else {
                                    Log.e(TAG, "No MediaView attached");
                                }
                            }
                        } catch (SecurityException se) {
                            Log.e("HomePageParent", "security exception accessing file URI");
                        }
                    } else {
                        Log.e("HomePageParent", "Null intent contents on file import; expected content URI");
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, intent);
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(CURRENT_MEDIA_PATH, mCurrentMediaPath);
        super.onSaveInstanceState(outState);
    }

    /*
     * Getter and setter for active media path.
     */
    @Override
    public String getCurrentMediaPath() {
        return mCurrentMediaPath;
    }

    @Override
    public void setCurrentMediaPath(String mCurrentMediaPath) {
        this.mCurrentMediaPath = mCurrentMediaPath;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public static class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case POSITION_CASES:
                    return LegalCaseFragment.newInstance();
                case POSITION_LAWYERS:
                    return LegalContactFragment.newInstance();
                case POSITION_NEWS:
                    return NewsFragment.newInstance();
                case POSITION_REPORT:
                    return ReportSubmissionFragment.newInstance();
                default:
                    break;
            }
            return null;
        }

        @Override
        public int getCount() {
            return COUNT;
        }
    }

    @Override
    protected void onDestroy() {
        if (updateReceiver != null) {
            unregisterReceiver(updateReceiver);
        }
        super.onDestroy();
    }
}
