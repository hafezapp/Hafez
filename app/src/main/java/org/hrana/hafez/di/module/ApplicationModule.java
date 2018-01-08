package org.hrana.hafez.di.module;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;

import org.hrana.hafez.di.scope.ApplicationScope;

import dagger.Module;
import dagger.Provides;

/**
 * App Module class
 */

@Module
public class ApplicationModule {

    private Application mApplication;

    public ApplicationModule(Application mApplication) {
        this.mApplication = mApplication;
    }

    @Provides @ApplicationScope
    Application providesApplication() {
        return mApplication;
    }

    @Provides @ApplicationScope
    ConnectivityManager provideConnectivityManager(Application application) {
        return (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Provides @ApplicationScope
    SharedPreferences provideSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(mApplication);
    }

}
