package org.hrana.hafez.di.component;

import org.hrana.hafez.adapter.NewsFeedAdapter;
import org.hrana.hafez.di.module.RssPresenterModule;
import org.hrana.hafez.di.scope.RssNewsScope;
import org.hrana.hafez.presenter.contract.IFeedPresenter;
import org.hrana.hafez.view.fragment.NewsFragment;

import dagger.Component;

/**
 * Adapter component interface for RSS Feed.
 */
@Component(modules = {RssPresenterModule.class}, dependencies = {IApplicationComponent.class})
@RssNewsScope
public interface IRssPresenterComponent {

    NewsFeedAdapter adapter();
    IFeedPresenter feedPresenter();

    void inject(NewsFragment fragment);

}
