package org.hrana.hafez.presenter;

import android.support.test.runner.AndroidJUnit4;

import org.hrana.hafez.model.RssEntry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * (Limited) test class for rss parser
 */
@RunWith(AndroidJUnit4.class)
public class FeedPresenterTest {

    private FeedPresenter feedPresenter;
    private InputStream inputStream, badInputStream;

    @Before
    public void setUp() throws Exception {
        feedPresenter = new FeedPresenter();
        inputStream = this.getClass().getClassLoader().getResourceAsStream("sample_rss_feed.txt");
        badInputStream = this.getClass().getClassLoader().getResourceAsStream("sample_broken_rss_feed.txt");
    }

    @After
    public void tearDown() throws Exception {
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
        if (badInputStream != null) {
            badInputStream.close();
            badInputStream = null;
        }
        feedPresenter = null;
    }

    @Test
    public void testParseBadFeed() throws Exception {
        List<RssEntry> mResult = feedPresenter.parse(badInputStream);
        Assert.assertEquals(0, mResult.size());
    }

    @Test
    public void testParseAllEntries() throws Exception {
        List<RssEntry> entries = feedPresenter.parse(inputStream);
        assertNotNull(entries);
        assertEquals(30, entries.size());
    }

    @Test
    public void testParseElements() throws Exception {
        List<RssEntry> entries = feedPresenter.parse(inputStream);
        for (RssEntry entry : entries) {
            if (entry.getDate() == null
                    || entry.getTitle() == null
                    || entry.getSummary() == null) {
                fail("Failed to parse all of entry attributes");
            }
        }
        assertNotNull(entries);
    }

    // return parser at correct location
    private XmlPullParser setParser(String targetTag) throws Exception {
        XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
        xppf.setNamespaceAware(true);
        XmlPullParser parser = xppf.newPullParser();
        parser.setInput(inputStream, null);
        parser.nextTag();

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name != null && name.equals(targetTag)) {
                return parser;
            }
        }
        return null;
    }

    @Test
    public void testReadEntry() throws Exception {
        XmlPullParser parser = setParser("item");
        if (parser != null) {
            RssEntry item = feedPresenter.readEntry(parser);
            assertNotNull(item);
        } else {
            fail("Null parser");
        }
    }

}
