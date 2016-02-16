package org.motechproject.atomclient.service.impl;

import com.rometools.fetcher.impl.FeedFetcherCache;
import com.rometools.fetcher.impl.SyndFeedInfo;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.SyndFeedOutput;
import org.motechproject.atomclient.domain.FeedRecord;
import org.motechproject.atomclient.repository.FeedRecordDataService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class FeedCache implements FeedFetcherCache {


    public static final String FEED_DATA = "feedData";
    private FeedRecordDataService feedRecordDataService;
    private EventRelay eventRelay;

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedCache.class);


    public FeedCache(FeedRecordDataService feedRecordDataService, EventRelay eventRelay) {
        this.feedRecordDataService = feedRecordDataService;
        this.eventRelay = eventRelay;
    }


    public static byte[] feedToBytes(SyndFeed feed) throws FeedException, UnsupportedEncodingException {
        SyndFeedOutput syndFeedOutput = new SyndFeedOutput();
        String s = syndFeedOutput.outputString(feed);
        return s.getBytes("UTF-8");
    }


    public static SyndFeed feedFromBytes(byte[] bytes) throws IOException, ClassNotFoundException, FeedException {
        String s = new String(bytes, "UTF-8");
        SyndFeedInput syndFeedInput = new SyndFeedInput();
        return syndFeedInput.build(new StringReader(s));
    }


    public static String urlToString(URL url) throws IOException {
        return url.toExternalForm();
    }


    public static URL urlFromString(String s) throws IOException {
        return new URL(s);
    }


    public SyndFeedInfo feedRecordToFeedInfo(FeedRecord record) throws IOException, ClassNotFoundException, FeedException {
        SyndFeedInfo info = new SyndFeedInfo();
        info.setUrl(urlFromString(record.getUrl()));
        info.setETag(record.getFeedETag());
        info.setId(record.getFeedId());
        info.setLastModified(record.getFeedLastModified());
        byte[] bytes = (byte[]) feedRecordDataService.getDetachedField(record, FEED_DATA);
        info.setSyndFeed(feedFromBytes(bytes));
        return info;
    }


    /**
     * Get a SyndFeedInfo object from the cache.
     *
     * @param feedUrl The url of the feed
     * @return A SyndFeedInfo or null if it is not in the cache
     */
    @Override
    @Transactional
    public SyndFeedInfo getFeedInfo(URL feedUrl) {
        try {
            String url = urlToString(feedUrl);
            FeedRecord record = feedRecordDataService.findByURL(url);
            if (record != null) {
                LOGGER.debug("*** found in cache *** {}", url);
                return feedRecordToFeedInfo(record);
            }
            LOGGER.debug("*** not in cache *** {}", url);
        } catch (IOException | ClassNotFoundException | FeedException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        return null;
    }


    /**
     * Sends a MOTECH event for a feed entry
     *
     * @param entry
     */
    private void sendMessageForFeedEntry(SyndEntry entry) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("uri", entry.getUri());
        if (entry.getContents() != null) {
            if (entry.getContents().size() == 1) {
                SyndContent content = entry.getContents().get(0);
                parameters.put("content", content.getValue());
            } else {
                int index = 1;
                for (SyndContent content : entry.getContents()) {
                    parameters.put(String.format("content%d", index), content.getValue());
                    index++;
                }
            }
        } else {
            LOGGER.warn("NULL content for entry {}", entry.getUri());
            parameters.put("content", null);
        }
        MotechEvent event = new MotechEvent(Constants.ATOMCLIENT_FEED_CHANGE_MESSAGE, parameters);
        LOGGER.debug("sending message {}", event);
        eventRelay.sendEventMessage(event);
    }


    /**
     * Sends a MOTECH event for each new feed entry
     *
     * @param feed
     */
    private void sendMessagesForNewFeedData(SyndFeed feed) {
        for (SyndEntry entry : feed.getEntries()) {
            sendMessageForFeedEntry(entry);
        }
    }


    /**
     * Sends a MOTECH event for each changed feed entry and returns true if changes were detected
     *
     * @param cachedFeed
     * @param fetchedFeed
     * @return true if any changes were detected between the cached entry and the fetched entry
     */
    private boolean sendMessagesForChangedEntries(SyndFeed cachedFeed, SyndFeed fetchedFeed) {
        boolean anyChanges = false;
        for (SyndEntry fetchedEntry : fetchedFeed.getEntries()) {
            for (SyndEntry cachedEntry : cachedFeed.getEntries()) {
                if (fetchedEntry.getUri().equals(cachedEntry.getUri())) {
                    if (fetchedEntry.getUpdatedDate().after(cachedEntry.getUpdatedDate())) {
                        LOGGER.debug("Entry {} was modified after the cached version, sending MOTECH event",
                                fetchedEntry.getUri());
                        sendMessageForFeedEntry(fetchedEntry);
                        anyChanges = true;
                    }
                }
            }
        }
        return anyChanges;
    }


    private FeedRecord recordFromFeed(String url, SyndFeedInfo info) throws IOException, FeedException {
        return new FeedRecord(
                url,
                info.getId(),
                urlToString(info.getUrl()),
                (Long) info.getLastModified(),
                info.getETag(),
                feedToBytes(info.getSyndFeed())
        );
    }


    /**
     * Add a SyndFeedInfo object to the cache
     *
     * @param feedUrl The url of the feed
     * @param feedInfo A SyndFeedInfo for the feed
     */
    @Override
    @Transactional
    public void setFeedInfo(URL feedUrl, SyndFeedInfo feedInfo) {
        try {
            String url = urlToString(feedUrl);
            LOGGER.debug("*** writing to cache *** {}", url);
            FeedRecord record = feedRecordDataService.findByURL(url);
            if (record != null) {
                //we're updating an existing cache element, why?
                LOGGER.debug("There's an existing record in the database for url {}", url);
                LOGGER.debug("feedInfo.getLastModified()   = {}", feedInfo.getLastModified());
                LOGGER.debug("record.getFeedLastModified() = {}", record.getFeedLastModified());
                SyndFeedInfo cachedFeedInfo = feedRecordToFeedInfo(record);
                if (sendMessagesForChangedEntries(cachedFeedInfo.getSyndFeed(), feedInfo.getSyndFeed())) {
                    feedRecordDataService.update(recordFromFeed(url, feedInfo));
                }
            } else {
                //Since we're adding an element to the cache, it must be new, let's broadcast this as a new feed
                sendMessagesForNewFeedData(feedInfo.getSyndFeed());
                feedRecordDataService.create(recordFromFeed(url, feedInfo));
            }
        } catch (IOException | FeedException | ClassNotFoundException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }


    /**
     * Removes all items from the cache.
     */
    @Override
    @Transactional
    public void clear() {
        feedRecordDataService.deleteAll();
    }


    /**
     * Removes the SyndFeedInfo identified by the url from the cache.
     *
     * @return The removed SyndFeedInfo
     */
    @Override
    @Transactional
    public SyndFeedInfo remove(URL feedUrl) {
        try {
            String url = urlToString(feedUrl);
            FeedRecord record = feedRecordDataService.findByURL(url);
            if (record == null) {
                LOGGER.error("Trying to remove feedRecord for inexistent URL {}", feedUrl);
                return null;
            }
            LOGGER.debug("*** removing from cache *** {}", url);
            byte[] bytes = (byte[]) feedRecordDataService.getDetachedField(record, FEED_DATA);
            feedRecordDataService.delete(record);
            return feedRecordToFeedInfo(record);
        } catch (IOException | ClassNotFoundException | FeedException ex) {
            LOGGER.error(ex.getMessage(), ex);
            return null;
        }
    }
}
