package org.motechproject.atomclient.unit;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import org.hamcrest.core.IsEqual;
import org.junit.Test;
import org.motechproject.atomclient.service.FeedCache;

import java.io.IOException;

import static org.junit.Assert.assertThat;

public class FeedCacheUnitTest {

    @Test
    public void verifyInfoToBytes() throws IOException, ClassNotFoundException, CloneNotSupportedException, FeedException {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("atom_1.0");
        feed.setUri("foobar");
        String json = FeedCache.feedToJson(feed);
        SyndFeed feed2 = FeedCache.feedFromJson(json);
        assertThat(feed2.getUri(), IsEqual.equalTo(feed.getUri()));
    }
}
