package org.hrana.hafez.view.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.hrana.hafez.R;
import org.hrana.hafez.adapter.NewsFeedAdapter;
import org.hrana.hafez.di.BaseApplication;
import org.hrana.hafez.di.component.DaggerIRssPresenterComponent;
import org.hrana.hafez.di.module.RssPresenterModule;
import org.hrana.hafez.exception.ForbiddenServiceException;
import org.hrana.hafez.model.RssEntry;
import org.hrana.hafez.presenter.ApiConstants;
import org.hrana.hafez.presenter.contract.IFeedPresenter;
import org.hrana.hafez.presenter.contract.IViewContract;
import org.hrana.hafez.view.activity.BaseActivity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongConnectionBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static org.hrana.hafez.Constants.ACTION_ERROR;
import static org.hrana.hafez.Constants.ACTION_VIEW;
import static org.hrana.hafez.Constants.CAN_ATTEMPT_ORBOT;
import static org.hrana.hafez.Constants.CATEGORY_ERROR;
import static org.hrana.hafez.Constants.CATEGORY_INTERNAL;

/**
 * Simple Newsfeed Fragment class.
 */
public class NewsFragment extends Fragment implements IViewContract.RssView,
        StrongBuilder.Callback<HttpURLConnection> {
    private static final String TAG = "NewsFragment";
    private Unbinder unbinder;
    private Subscription mSubscription;
    private final String URL = "https://www.hra-news.org/feed",
            ENTRIES = "entries";
    private final int WEB_INTENT_CODE = 460;

    @Inject
    IFeedPresenter feedPresenter;
    @Inject
    NewsFeedAdapter adapter;
    @Inject
    SharedPreferences preferences;
    @Inject
    ConnectivityManager connectivityManager;

    @BindView(R.id.fragment_title)
    TextView title;
    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;
    @BindView(R.id.offline_view)
    LinearLayout offlineView;

    public static NewsFragment newInstance() {
        return new NewsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // DI
        DaggerIRssPresenterComponent.builder()
                .rssPresenterModule(new RssPresenterModule(this))
                .iApplicationComponent(BaseApplication.get(getActivity())
                        .getComponent())
                .build()
                .inject(this);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_recycler, container, false);
        unbinder = ButterKnife.bind(this, v);
        title.setText(getString(R.string.news));

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        if (savedInstanceState != null) {
            ArrayList<RssEntry> entries = savedInstanceState.getParcelableArrayList(ENTRIES);
            if (entries != null) {
                feedPresenter.setEntries(entries);
            }
        }

        if (feedPresenter.hasRecentCache()) {
            adapter.addItems(feedPresenter.getCachedFeeds());
            adapter.notifyDataSetChanged();
        } else { // Have to request news items again
            if (hasNetworkAccess()) {
                loadFeed(ApiConstants.FEED_URL);
            } else {
                showNoNetworkAccess();
            }
        }
        return v;
    }

    @Override
    public void onConnected(HttpURLConnection httpURLConnection) {
        mSubscription = feedPresenter.getRssFeed(httpURLConnection)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RssFeedObserver());
    }

    @Override
    public void onConnectionException(Exception e) {
        Log.e(TAG, "ConnectionException");
    }

    @Override
    public void onTimeout() {
        Log.e(TAG, "timeout");
        if (!preferences.getBoolean(getString(R.string.restrict_access_tor_key), false)) {
            Log.d(TAG, "Loading without Tor since allowed by user preferences");
            loadFeedFallback(ApiConstants.FEED_URL);
        } else {
            Log.d(TAG, "Access restricted to Tor only");
        }
    }

    @Override
    public void onInvalid() {
        Log.e(TAG, "onInvalid");
        showNoNetworkAccess();
    }

    /*
     * @TODO: ***
     * If the user doesn't have a proxy running, trying to view the newsfeed links in their
     * browser will probably not work because the content will be blocked.
     */
    @Override
    public void handleFeedClick(final String url) {
        Intent webIntent = new Intent();
        webIntent.setData(Uri.parse(url));
        getActivity().startActivityForResult(webIntent, WEB_INTENT_CODE);
    }

    // Expose presenter to main activity to check for feeds
    public IFeedPresenter getFeedPresenter() {
        return feedPresenter;
    }

    private void showNoNetworkAccess() {
        if (offlineView != null && recyclerView != null) {
            offlineView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    /*
     * True if network access detected and access complies with user preferences (i.e. restrictions on mobile data use)
     */
    private boolean hasNetworkAccess() {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null
                && activeNetworkInfo.isConnectedOrConnecting();
    }

    /*
     * Load RSS Feed from Hrana-news website. If Orbot is installed and Orbot use is enabled, attempts to load feed over Tor.
     * If Orbot is not installed and user has not requested that access to Hrana-News website
     * be restricted, loads feed without Orbot.
     *
     */
    public void loadFeed(String targetUrl) {
        if (OrbotHelper.isOrbotInstalled(getContext()) && CAN_ATTEMPT_ORBOT) {
            try {
                StrongConnectionBuilder.forMaxSecurity(getActivity())
                        .withTorValidation()
                        .withWeakCiphers()
                        .withBestProxy()
                        .connectTo(new URL(targetUrl))
                        //.withTrustManagers() //@TODO pin cert here if re-enabling Orbot/Tor connections
                        .build(this);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
                showError();
            }
        } else if (!preferences.getBoolean
                (getString(R.string.restrict_access_tor_key), false)) { // Currently, Tor/Orbot not being used
            loadFeedFallback(targetUrl);
        } else {
            showNoNetworkAccess();
        }
    }

    /*
     * Asynchronous retrieval of RSS feed, no netcipher option.
     */
    @Override
    public void loadFeedFallback(String targetUrl) {
        mSubscription = feedPresenter.getRssFeed(targetUrl)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RssFeedObserver());
    }

    @Override
    public void showError() {
        ((BaseActivity) getActivity()).sendAnalyticsHitEvent(ACTION_VIEW, CATEGORY_INTERNAL, "No Network Access View");
        showNoNetworkAccess();
    }

    /*
     * Handle results of feed fetching
     */
    private class RssFeedObserver implements Observer<List<RssEntry>> {
        private boolean hasRetried;

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            ((BaseActivity) getActivity()).sendAnalyticsHitEvent(ACTION_ERROR, CATEGORY_ERROR, e.getMessage());
            if (e instanceof ForbiddenServiceException
                    && !preferences.getBoolean
                    (getString(R.string.restrict_access_tor_key), false)
                    && !hasRetried) {

                // Fallback without Tor if permitted
                loadFeedFallback(ApiConstants.FEED_URL);
                hasRetried = true;
            } else {
                showError();
            }
        }

        @Override
        public void onNext(List<RssEntry> rssEntries) {
            offlineView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.addItems(rssEntries);
            feedPresenter.setEntries(rssEntries);
            adapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(ENTRIES, (ArrayList<? extends Parcelable>)
                feedPresenter.getCachedFeeds());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        if (mSubscription != null
                && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
        super.onDestroyView();
    }
}
