package org.hrana.hafez.di.component;

import org.hrana.hafez.di.module.PresenterModule;
import org.hrana.hafez.di.scope.ActivityScope;
import org.hrana.hafez.view.activity.MainActivity;
import org.hrana.hafez.view.activity.SettingsActivity;

import dagger.Component;


@Component(modules={PresenterModule.class}, dependencies = {IApplicationComponent.class})
@ActivityScope
public interface IPresenterComponent {
    void inject(MainActivity activity);
    void inject(SettingsActivity.FirstPreferenceFragment fragment);
    void inject(SettingsActivity activity);
}
