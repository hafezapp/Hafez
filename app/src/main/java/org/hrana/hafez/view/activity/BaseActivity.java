package org.hrana.hafez.view.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.hrana.hafez.di.BaseApplication;
import org.hrana.hafez.util.LocaleUtils;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * BaseActivity class for handling custom locale.
 */

public abstract class BaseActivity extends AppCompatActivity {
    public BaseActivity() {
        LocaleUtils.updateLocale(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        View view = super.onCreateView(name, context, attrs);
        if (view != null) {
            view.setFilterTouchesWhenObscured(true);
        }
        return view;
    }

    @Override
    public void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void setAnalyticsScreenName(String name) {
        Tracker tracker = ((BaseApplication) getApplication()).getDefaultTracker();
        tracker.setScreenName(name);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void sendAnalyticsHitEvent(String action, String category, String label) {
        Tracker tracker = ((BaseApplication) getApplication()).getDefaultTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setAction(action)
                .setCategory(category)
                .setLabel(label)
                .build());
    }

}
