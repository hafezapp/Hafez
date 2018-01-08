package org.hrana.hafez.di.module;

import org.hrana.hafez.adapter.NewsFeedAdapter;
import org.hrana.hafez.di.scope.RssNewsScope;
import org.hrana.hafez.presenter.FeedPresenter;
import org.hrana.hafez.presenter.contract.IFeedPresenter;
import org.hrana.hafez.presenter.contract.IViewContract;

import dagger.Module;
import dagger.Provides;

/**
 * Module for RSS presenter and adapter dependencies.
 */
@Module
public class RssPresenterModule {
    private IViewContract.RssView view;

    public RssPresenterModule(IViewContract.RssView view) {
        this.view = view;
    }

    @Provides
    @RssNewsScope
    NewsFeedAdapter provideNewsFeedAdapter() {
        return new NewsFeedAdapter(view);
    }

    @Provides @RssNewsScope
    IFeedPresenter provideFeedPresenter() {
        return new FeedPresenter();
    }

}
