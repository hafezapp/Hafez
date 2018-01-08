package org.hrana.hafez.presenter;

import android.os.Build;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import org.hrana.hafez.BuildConfig;
import org.hrana.hafez.Constants;
import org.hrana.hafez.exception.ForbiddenServiceException;
import org.hrana.hafez.model.RssEntry;
import org.hrana.hafez.presenter.contract.IFeedPresenter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
import retrofit2.Retrofit;
import rx.Observable;
import rx.Subscriber;

import static org.hrana.hafez.Constants.PROD_HOSTNAME;

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
            ENTRIES = "entries",
            TITLE = "title",
            DATE = "updated",
            LINK = "url",
            DESCRIPTION = "shortSummary",
            CONTENT = "content";

    /*
     * Cache is an in-memory cache of up to 15min if the app is active.
     * If a more persistent cache is desired using a database, CacheWord + SqlCipher or similar
     * is recommended.
     */
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

                    //URL inputUrl = new URL(url); //@todo cert pin
                    InputStream inputStream = call.execute().body().byteStream(); // closed in calling method
                    entries = parse(inputStream);
                    lastRequestTime = new Date(); // 'timestamp'
                    subscriber.onNext(entries);
                } catch (IOException ex) {
                    Log.e("FeedPresenter", ex.getMessage());
                    subscriber.onError(ex);
                    lastRequestTime = null;
                }
            }
        });
    }

    protected List<RssEntry> parse(InputStream inputStream) throws IOException {
        List<RssEntry> entries = new ArrayList<>();
        JsonReader jr = null;
        try {
            jr = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            jr.beginObject();
            while (jr.hasNext()) {
                JsonToken peek = jr.peek();
                if (peek == JsonToken.NULL || peek != JsonToken.NAME) {
                    jr.skipValue();
                } else {
                    String tag = jr.nextName();
                    if (ENTRIES.equals(tag)) {
                        jr.beginArray();
                        while (jr.hasNext()) {
                            entries.add(readEntry(jr));
                        }
                        jr.endArray();
                    }
                }
            }
            jr.endObject();
        } finally {
            if (jr != null) {
                jr.close();
            }
            inputStream.close();
        }
        return entries;
    }

    /*
     * Parse an RSS entry into an RssEntry object
     * @param   reader  JsonReader
     * @returns         RssEntry
     */
    private RssEntry readEntry(JsonReader reader) throws IOException {
        String title = null, date = null, summary = null, expanded = null, url = null;
        reader.beginObject();
        while (reader.hasNext()) {
            JsonToken peek = reader.peek();
            if (peek == JsonToken.NULL || peek != JsonToken.NAME) {
                reader.skipValue();
            } else {
                String tag = reader.nextName();

                if (TITLE.equals(tag)) {
                    JsonToken check = reader.peek();
                    if (check != JsonToken.NULL) {
                        title = reader.nextString();
                    }
                } else if (DATE.equals(tag)) {
                    JsonToken check = reader.peek();
                    if (check != JsonToken.NULL) {
                        date = reader.nextString();
                    }
                } else if (DESCRIPTION.equals(tag)) {
                    JsonToken check = reader.peek();
                    if (check != JsonToken.NULL) {
                        summary = reader.nextString();
                    }
                } else if (LINK.equals(tag)) {
                    JsonToken check = reader.peek();
                    if (check != JsonToken.NULL) {
                        url = reader.nextString();
                    }
                } else if (CONTENT.equals(tag)) {
                    JsonToken check = reader.peek();
                    if (check != JsonToken.NULL) {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            if (reader.nextName().equals("html")) {
                                expanded = removeHtmlTags(reader.nextString()); // remove html tags and replace paragraph breaks with line separators.
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                    }
                }
            }
        }
        reader.endObject();

        return RssEntry.builder()
                .date(date)
                .title(title)
                .summary(summary)
                .url(url)
                .expandedText(expanded)
                .build();
    }

    private String removeHtmlTags(String result) throws IOException {

        // Remove html tags and elements from article text that can't be displayed in Android.
        // This isn't ideal.
        return result
                .replaceAll("<(/)figure>|(<figure.+?>)|(<(/)img>)|(<img.+?>)|<p>|<a.+?>|&#[1-9+];", "")
                .replaceAll("</p>|</a>", System.getProperty("line.separator"));
    }

}
