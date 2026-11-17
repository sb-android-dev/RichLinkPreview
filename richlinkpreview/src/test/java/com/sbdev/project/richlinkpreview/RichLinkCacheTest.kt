package com.sbdev.project.richlinkpreview

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class RichLinkCacheTest {

    private fun meta(title: String) = MetaData(title = title)

    @Before
    fun setUp() {
        RichLinkCache.enabled = true
        RichLinkCache.clear()
        RichLinkCache.maxSize = 100
    }

    private var now = 0L

    @Before
    fun useFakeClock() {
        now = 1_000_000L
        RichLinkCache.clock = { now }
        RichLinkCache.expireAfter = 1.hours
    }

    @After
    fun tearDown() {
        RichLinkCache.enabled = false
        RichLinkCache.maxSize = 100
        RichLinkCache.expireAfter = 1.hours
        RichLinkCache.clock = { System.nanoTime() / 1_000_000 }
    }

    @Test
    fun `entries expire after expireAfter`() {
        RichLinkCache.expireAfter = 10.minutes
        RichLinkCache.put("a", meta("A"))

        now += 9.minutes.inWholeMilliseconds
        assertEquals("A", RichLinkCache.get("a")?.title)

        now += 1.minutes.inWholeMilliseconds
        assertNull(RichLinkCache.get("a"))
        assertEquals(0, RichLinkCache.size) // the expired entry was dropped on read
    }

    @Test
    fun `putting again restarts the expiry`() {
        RichLinkCache.expireAfter = 10.minutes
        RichLinkCache.put("a", meta("old"))
        now += 8.minutes.inWholeMilliseconds
        RichLinkCache.put("a", meta("new"))
        now += 8.minutes.inWholeMilliseconds
        assertEquals("new", RichLinkCache.get("a")?.title)
    }

    @Test
    fun `infinite expiry never expires`() {
        RichLinkCache.expireAfter = Duration.INFINITE
        RichLinkCache.put("a", meta("A"))
        now += 365.days.inWholeMilliseconds
        assertEquals("A", RichLinkCache.get("a")?.title)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `expireAfter must be positive`() {
        RichLinkCache.expireAfter = Duration.ZERO
    }

    @Test
    fun `nothing is cached while disabled`() {
        RichLinkCache.enabled = false
        RichLinkCache.put("a", meta("A"))
        assertNull(RichLinkCache.get("a"))
        assertEquals(0, RichLinkCache.size)
    }

    @Test
    fun `disabling clears what was cached`() {
        RichLinkCache.put("a", meta("A"))
        RichLinkCache.enabled = false
        RichLinkCache.enabled = true
        assertNull(RichLinkCache.get("a"))
    }

    @Test
    fun `stores and returns metadata by url`() {
        RichLinkCache.put("https://a.com", meta("A"))
        assertEquals("A", RichLinkCache.get("https://a.com")?.title)
        assertNull(RichLinkCache.get("https://b.com"))
    }

    @Test
    fun `evicts the least recently used entry when full`() {
        RichLinkCache.maxSize = 2
        RichLinkCache.put("a", meta("A"))
        RichLinkCache.put("b", meta("B"))
        RichLinkCache.get("a") // "a" is now more recent than "b"
        RichLinkCache.put("c", meta("C"))

        assertNull(RichLinkCache.get("b"))
        assertEquals("A", RichLinkCache.get("a")?.title)
        assertEquals("C", RichLinkCache.get("c")?.title)
    }

    @Test
    fun `lowering maxSize trims the oldest entries`() {
        listOf("a", "b", "c").forEach { RichLinkCache.put(it, meta(it)) }
        RichLinkCache.maxSize = 1
        assertEquals(1, RichLinkCache.size)
        assertEquals("c", RichLinkCache.get("c")?.title)
    }

    @Test
    fun `maxSize zero disables caching`() {
        RichLinkCache.maxSize = 0
        RichLinkCache.put("a", meta("A"))
        assertNull(RichLinkCache.get("a"))
    }

    @Test
    fun `user agent is part of the key`() {
        RichLinkCache.put("a", meta("default"))
        RichLinkCache.put("a", meta("mobile"), userAgent = "Mobile")
        assertEquals("default", RichLinkCache.get("a")?.title)
        assertEquals("mobile", RichLinkCache.get("a", userAgent = "Mobile")?.title)
    }

    @Test
    fun `remove and clear drop entries`() {
        RichLinkCache.put("a", meta("A"))
        RichLinkCache.put("b", meta("B"))
        RichLinkCache.remove("a")
        assertNull(RichLinkCache.get("a"))
        RichLinkCache.clear()
        assertEquals(0, RichLinkCache.size)
    }
}
