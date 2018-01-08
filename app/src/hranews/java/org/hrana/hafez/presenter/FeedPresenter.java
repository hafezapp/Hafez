package org.hrana.hafez.presenter;

import android.util.Log;

import org.hrana.hafez.BuildConfig;
import org.hrana.hafez.exception.ForbiddenServiceException;
import org.hrana.hafez.model.RssEntry;
import org.hrana.hafez.presenter.contract.IFeedPresenter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.CertificatePinner;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import rx.Observable;
import rx.Subscriber;

/**
 * Presenter for RSS Feeds. Thanks to https://github.com/andropenguin/SladMobileRssReader project
 * for Observable#call implementation.
 */
public class FeedPresenter implements IFeedPresenter {
    private static final String TAG = "FeedPresenter";
    private List<RssEntry> entries;
    private Date lastRequestTime;
    private final long CACHE_TIME = 15 * 60 * 1000; // 15 min
    private final String
            CHANNEL = "channel",
            ITEM = "item",
            TITLE = "title",
            DATE = "pubDate",
            LINK = "link",
            DESCRIPTION = "description", // there are 2 description tags in feed, one child of item one not--not sure if problem
            CONTENT = "encoded", // only if expanded view in-line.
            PARAGRAPH = "p",
            NAMESPACE = null;

    // If a request has been made in the last 15 min
    public boolean hasRecentCache() {
        return (entries != null && !entries.isEmpty() &&
                lastRequestTime != null &&
                new Date().getTime() - lastRequestTime.getTime() <= CACHE_TIME);
    }

    public List<RssEntry> getCachedFeeds() {
        return entries;
    }

    public void setEntries(List<RssEntry> newEntries) {
        this.entries = newEntries;
    }

    @Override
    public Observable<List<RssEntry>> getRssFeed(final HttpURLConnection conn) {
        return Observable.create(new Observable.OnSubscribe<List<RssEntry>>() {
            @Override
            public void call(Subscriber<? super List<RssEntry>> subscriber) {
                try {
                    int status = conn.getResponseCode();
                    if (status >= 200 && status < 300) {
                        InputStream inputStream = conn.getInputStream();
                        entries = parse(inputStream);
                        lastRequestTime = new Date(); // 'timestamp'
                        subscriber.onNext(entries);
                    } else if (status == 403) {
                        subscriber.onError(new ForbiddenServiceException("403 error"));
                    } else {
                        //InputStream is = conn.getErrorStream();
                        subscriber.onError(new IOException
                                ("Unsuccessful request " + "(" + status + ")"));
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "Exception in getRssFeed" + ex.getMessage());
                    lastRequestTime = null;
                    subscriber.onError(ex);
                } finally {
                    conn.disconnect();
                }
            }
        });
    }


    @Override
    public Observable<List<RssEntry>> getRssFeed(final String url) {
        return Observable.create(new Observable.OnSubscribe<List<RssEntry>>() {
            @Override
            public void call(Subscriber<? super List<RssEntry>> subscriber) {
                try {

                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .addInterceptor(new HttpLoggingInterceptor()
                                    .setLevel(BuildConfig.DEBUG
                                            ? HttpLoggingInterceptor.Level.BASIC
                                            : HttpLoggingInterceptor.Level.NONE))
                            .certificatePinner(new CertificatePinner.Builder()
                                    //@TODO: cert pinning: add the sha256 hash here once URL and cert are finalized.
                                    //.add(ApiConstants.FEED_DOMAIN, "")
                                    .build())
                            .connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS)) // no cleartext, no older TLS fallbacks.
                            .build();

                    Call call = client.newCall(new Request.Builder().url(ApiConstants.FEED_URL).build());

                    InputStream inputStream = call.execute().body().byteStream(); // closed in calling method
                    entries = parse(inputStream);
                    lastRequestTime = new Date(); // 'timestamp'
                    subscriber.onNext(entries);
                } catch (IOException ex) {
                    subscriber.onError(ex);
                    lastRequestTime = null;
                }
            }
        });
    }

    // Thanks to tutorial documentation at https://developer.android.com/training/basics/network-ops/xml.html
    // Can't throw PullParserException here, because the feed is poorly formatted. We need to swallow it to allow any parsing to occur.
    protected List<RssEntry> parse(InputStream inputStream) throws IOException {
        List<RssEntry> entries = null;
        try {
            XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
            xppf.setNamespaceAware(true);
            XmlPullParser parser = xppf.newPullParser();
            parser.setInput(inputStream, null);
            parser.nextTag();
            entries = readFeed(parser);
        } catch (XmlPullParserException ex) {
            Log.e(TAG,"Error parsing feed: " + ex.getMessage());
        } finally {
            inputStream.close();
        }
        return entries;
    }

    protected List<RssEntry> readFeed(XmlPullParser parser) throws IOException {
        List<RssEntry> entries = new ArrayList<>();
        try {
            parser.require(XmlPullParser.START_TAG, NAMESPACE, "rss");
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if (name != null && name.equals(ITEM)) {
                    entries.add(readEntry(parser));
                }
            }
            // Stop parsing but return correctly-parsed feed items, if any
        } catch (XmlPullParserException ex) {
            Log.e(TAG, ex.getMessage());
        }
        return entries;
    }

    /*
     * Parse an RSS entry into an RssEntry object
     * @param   parser  XmlPullParser object
     * @returns         RssEntry object
     */
    protected RssEntry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, ITEM);
        String title = null, date = null, summary = null, expanded = null, url = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue; // get to next start of content tag
            }

            final String tag = parser.getName();
            switch (tag) {
                // act based on type of tag
                case TITLE:
                    title = readTag(parser, TITLE);
                    break;
                case DATE:
                    date = readTag(parser, DATE);
                    break;
                case DESCRIPTION:
                    summary = readTag(parser, DESCRIPTION);
                    break;
                case LINK:
                    url = readTag(parser, LINK); // try
                    //url = readUrl(parser);
                    break;
                case CONTENT:
                    expanded = readTag(parser, CONTENT); // return cdata-formatted
                    break;
                case PARAGRAPH: // what happens to paragraph tag, <a rel...> tag, etc?
                    // keep reading - we will have to remove these tags in the text.
                    break;
                default: // some category or tag we're not using
                    skip(parser);
                    break;
            }
        }
        return RssEntry.builder()
                .date(date)
                .title(title)
                .summary(summary)
                .url(url)
                .expandedText(expanded)
                .build();
    }

    /*
     * Read text inside given tag and return as a string.
     * @param tag   target tag
     * @returns     enclosed text
     */
    protected String readTag(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, tag);
        String text = readText(parser);
        parser.require(XmlPullParser.END_TAG, NAMESPACE, tag);
        return text;
    }

    protected String readUrl(XmlPullParser parser) throws IOException {
        try {
            parser.require(XmlPullParser.START_TAG, NAMESPACE, LINK);
            String link = "";
            String tag = parser.getName();
            String relType = parser.getAttributeValue(null, "rel");
            if (tag.equals("link")) {
                if (relType != null && relType.equals("alternate")) {
                    link = parser.getAttributeValue(null, "href");
                    parser.nextTag();
                } else {
                    link = readText(parser); //?
                }
            }
            parser.require(XmlPullParser.END_TAG, NAMESPACE, LINK);
            return link;
        } catch (XmlPullParserException ex) {
            Log.e(TAG, ex.getMessage());
        }
        return null;
    }

    protected String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }

        // Clean paragraph tags.
        return result
                .replaceAll("<p>", "")
                .replaceAll("</p>", System.getProperty("line.separator"))
                .replaceAll("&#[1-9+];", ""); // not ideal but if html codes didn't work out...
    }

    protected void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
