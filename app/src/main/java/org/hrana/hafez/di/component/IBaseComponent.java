package org.hrana.hafez.di.component;

import android.app.IntentService;

import org.hrana.hafez.di.module.BaseModule;
import org.hrana.hafez.di.scope.BaseScope;

import dagger.Component;

@BaseScope
@Component(modules= BaseModule.class, dependencies = IApplicationComponent.class)
public interface IBaseComponent {

    void inject(IntentService service);
}
