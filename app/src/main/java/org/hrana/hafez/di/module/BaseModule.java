package org.hrana.hafez.di.module;

import android.content.SharedPreferences;

import org.hrana.hafez.di.scope.BaseScope;

import dagger.Module;
import dagger.Provides;


@Module
public class BaseModule {

    @Provides @BaseScope
    SharedPreferences prefs(SharedPreferences preferences) {
        return preferences;
    }

}
