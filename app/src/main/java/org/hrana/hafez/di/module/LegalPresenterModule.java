package org.hrana.hafez.di.module;


import org.hrana.hafez.adapter.LegalContactsAdapter;
import org.hrana.hafez.di.scope.ActivityScope;
import org.hrana.hafez.presenter.contract.IViewContract;
import org.hrana.hafez.presenter.LegalContactPresenter;

import dagger.Module;
import dagger.Provides;

/**
 * Dependency injection provider.
 */

@Module
public class LegalPresenterModule {

    private IViewContract.LegalContactsView view;
    public LegalPresenterModule(IViewContract.LegalContactsView view) {
        this.view = view;
    }

    @Provides @ActivityScope
    LegalContactPresenter provideLegalConntactPresenter() {
        return new LegalContactPresenter();
    }

    @Provides @ActivityScope
    LegalContactsAdapter provideLegalContactsAdapter() {
        return new LegalContactsAdapter(view);
    }

}
