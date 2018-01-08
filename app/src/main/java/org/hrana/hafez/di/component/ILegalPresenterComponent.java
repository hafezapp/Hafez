package org.hrana.hafez.di.component;

import org.hrana.hafez.adapter.LegalContactsAdapter;
import org.hrana.hafez.di.module.LegalPresenterModule;
import org.hrana.hafez.di.scope.ActivityScope;
import org.hrana.hafez.presenter.LegalContactPresenter;
import org.hrana.hafez.view.fragment.LegalContactFragment;
import org.hrana.hafez.view.fragment.NewsFragment;

import dagger.Component;


/**
 * General presenter component interface. Does not include dependencies for
 * {@link NewsFragment}, which has separate module and component.
 */

@Component(modules = {LegalPresenterModule.class}, dependencies = {IApplicationComponent.class})
@ActivityScope
public interface ILegalPresenterComponent {

    LegalContactsAdapter adapter();
    LegalContactPresenter presenter();
    void inject(LegalContactFragment fragment);

}
