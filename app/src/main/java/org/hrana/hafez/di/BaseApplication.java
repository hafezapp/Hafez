package org.hrana.hafez.di;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.hrana.hafez.Constants;
import org.hrana.hafez.di.component.DaggerIApplicationComponent;
import org.hrana.hafez.di.component.IApplicationComponent;
import org.hrana.hafez.di.module.ApplicationModule;
import org.hrana.hafez.service.update.AmazonJobService;
import org.hrana.hafez.util.LocaleUtils;

import java.util.Locale;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import io.fabric.sdk.android.Fabric;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

import static org.hrana.hafez.Constants.CAN_ATTEMPT_ORBOT;
import static org.hrana.hafez.Constants.TRACKER_ID;


/**
 * Custom application subclass.
 */
public class BaseApplication extends Application {
    private IApplicationComponent appComponent;
    private Tracker mTracker;
    private final String GOOGLE_MARKET = "com.android.vending";

    public static BaseApplication get(Context context) {
        return (BaseApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        // font
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/iransans.ttf")
                .setFontAttrId(org.hrana.hafez.R.attr.fontPath)
                .build()
        );

        appComponent = DaggerIApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
        appComponent.inject(this);

        // Farsi language enforced
        setLocale();

        // Set up OrbotHelper to manage async communication with orbot
        if (CAN_ATTEMPT_ORBOT) { // For now, in-app Orbot integration is not enabled.
            OrbotHelper.get(this).init();
        }

        // If app not downloaded from google play, check for updates
        String installer = getPackageManager().getInstallerPackageName(getPackageName());
        if (installer == null || !installer.equals(GOOGLE_MARKET)) {
            Intent checkUpdateIntent = new Intent();
            checkUpdateIntent.setAction(Constants.INTENT_ACTION_CHECK_VERSION_CODE);
            AmazonJobService.enqueueWork(this, checkUpdateIntent);
        }
    }

    public IApplicationComponent getComponent() {
        return appComponent;
    }

    private void setLocale() {
        LocaleUtils.setLocale(new Locale("fa"));
        LocaleUtils.updateLocale(this, getBaseContext().getResources().getConfiguration());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleUtils.updateLocale(this, newConfig);
    }

    /**
     * Snippet from developers.google.com:
     * Get default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    public synchronized Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mTracker = analytics.newTracker(TRACKER_ID);
            mTracker.enableExceptionReporting(true);
            mTracker.setAnonymizeIp(true);
        }
        return mTracker;
    }

}
