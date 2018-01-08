package org.hrana.hafez.adapter;

import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hrana.hafez.R;
import org.hrana.hafez.model.RssEntry;
import org.hrana.hafez.presenter.contract.IViewContract;
import org.hrana.hafez.util.LocaleUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Simple Adapter for RSS Feed.
 * Takes {@link IViewContract.RssView} as parameter to respond to view interactions.
 */
public class NewsFeedAdapter extends RecyclerView.Adapter<NewsFeedAdapter.ViewHolder> {
    private static final String TAG = "NewsFeedAdapter";
    private List<RssEntry> mDataset;
    private final IViewContract.RssView fragment;

    public NewsFeedAdapter(IViewContract.RssView view) {
        this.mDataset = new ArrayList<>();
        this.fragment = view;
    }

    public void addItems(List<RssEntry> items) {
        mDataset.addAll(items);
    }

    @Override
    public NewsFeedAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rss, parent, false);

        return new ViewHolder(view, fragment);
    }

    @Override @SuppressWarnings("deprecation") // version-checked
    public void onBindViewHolder(NewsFeedAdapter.ViewHolder holder, int position) {
        holder.mTitle.setText(mDataset.get(position).getTitle());
        holder.mDate.setText(LocaleUtils.getRelativeTimeSpan(mDataset.get(position).getDate()));
        holder.mUrl = mDataset.get(position).getUrl();

        //Html#fromHtml() is deprecated, requiring sdk version check before formatting
        String summary = mDataset.get(position).getSummary();
        if (summary != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                holder.description.setText(Html.fromHtml(summary, Html.FROM_HTML_MODE_LEGACY));
            } else {
                holder.description.setText(Html.fromHtml(summary));
            }
        }

        String fulltext = mDataset.get(position).getExpandedText();
        if (fulltext != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                holder.fulltext.setText(Html.fromHtml(fulltext, Html.FROM_HTML_MODE_LEGACY));
            } else {
                holder.fulltext.setText(Html.fromHtml(fulltext));
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mDataset != null) {
            return mDataset.size();
        }
        return 0;
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.title)
        TextView mTitle;
        @BindView(R.id.description)
        TextView description;
        @BindView(R.id.date)
        TextView mDate;
        @BindView(R.id.fulltext) TextView fulltext;
        private String mUrl;
        private IViewContract.RssView view;

        public ViewHolder(View itemView, IViewContract.RssView view) {
            super(itemView);
            this.view = view;
            ButterKnife.bind(this, itemView);
        }

        @OnClick() // Toggle fulltext and preview visibility, or open a link if fulltext could not be parsed.
        public void toggleFullTextDisplay() {
            if (mUrl != null) { // load in webview, or ask about loading, if http.
                if (fulltext != null
                        && !fulltext.getText().toString().isEmpty()) {
                    boolean isArticleVisible = fulltext.getVisibility() == View.VISIBLE;
                    fulltext.setVisibility(isArticleVisible
                            ? View.GONE
                            : View.VISIBLE);
                    description.setVisibility(isArticleVisible
                            ? View.VISIBLE
                            : View.GONE);
                } else { // no text showing, have to go to external site to try and read whole article.
                    view.handleFeedClick(mUrl);
                }
            }
        }
    }

}
