package com.sbdev.project.richlinkpreview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

/**
 * Written in Java on purpose: if the public API stops being usable from Java
 * (for example a member loses @JvmStatic), this file no longer compiles.
 */
public class JavaApiTest {

    @After
    public void tearDown() {
        RichLinkCache.setEnabled(false);
        RichLinkCache.setMaxSize(100);
        RichLinkCache.setExpireAfter(1, TimeUnit.HOURS);
    }

    @Test
    public void cacheIsConfigurableFromJava() {
        RichLinkCache.setEnabled(true);
        RichLinkCache.setMaxSize(50);
        RichLinkCache.setExpireAfter(30, TimeUnit.MINUTES);

        assertTrue(RichLinkCache.getEnabled());
        assertEquals(50, RichLinkCache.getMaxSize());
        assertEquals(TimeUnit.MINUTES.toMillis(30), RichLinkCache.getExpireAfterMillis());

        MetaData metaData = new MetaData("example.com", "", "Example", "", "", "", "", "https://example.com");
        RichLinkCache.put("https://example.com", metaData);
        assertEquals("Example", RichLinkCache.get("https://example.com").getTitle());

        RichLinkCache.remove("https://example.com");
        assertNull(RichLinkCache.get("https://example.com"));
    }

    @Test
    public void neverExpiringFromJava() {
        RichLinkCache.setExpireAfter(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        assertEquals(Long.MAX_VALUE, RichLinkCache.getExpireAfterMillis());
    }

    /** Not run (the callback is delivered on Android's main thread); it only has to compile. */
    @SuppressWarnings("unused")
    private void refreshCompilesFromJava() {
        List<String> urls = Arrays.asList("https://a.com", "https://b.com");
        RichLinks.refresh(urls, failedUrls -> {
            // e.g. swipeRefreshLayout.setRefreshing(false)
        });
        RichLinks.refresh(urls, failedUrls -> { }, "MyUserAgent");
    }
}
