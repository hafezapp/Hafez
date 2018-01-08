package org.hrana.hafez.di.component;

import android.app.Application;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import org.hrana.hafez.di.BaseApplication;
import org.hrana.hafez.di.module.ApplicationModule;
import org.hrana.hafez.di.scope.ApplicationScope;

import dagger.Component;

/**
 * Dependency injection graph
 */
@ApplicationScope
@Component(modules = {ApplicationModule.class})
public interface IApplicationComponent {

    Application application();
    ConnectivityManager connectivityManager();
    SharedPreferences sharedPreferences();

    void inject(BaseApplication application);
}
