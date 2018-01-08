package org.hrana.hafez.presenter.contract;
import org.hrana.hafez.model.RssEntry;

import java.net.HttpURLConnection;
import java.util.List;
import rx.Observable;

/**
 * Data presenter interface contracts
 */
public interface IFeedPresenter {

    Observable<List<RssEntry>> getRssFeed(final String url);
    Observable<List<RssEntry>> getRssFeed(final HttpURLConnection conn);
    boolean hasRecentCache();
    void setEntries(List<RssEntry> entries);
    List<RssEntry> getCachedFeeds();
}
