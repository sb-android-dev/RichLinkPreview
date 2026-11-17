package com.sbdev.project.richlinkpreview

import kotlin.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

/**
 * In-memory cache of fetched [MetaData], shared by every preview in the process.
 *
 * Previews in a `LazyColumn` or `RecyclerView` are recreated each time they scroll back
 * into view. With the cache, a URL that was already fetched is shown immediately instead
 * of being downloaded again. Only successful results are cached, so a failed URL is
 * retried next time.
 *
 * An entry is dropped when it is older than [expireAfter] (so a page that changed is
 * fetched again), or when it is the least recently used one and [maxSize] is reached.
 * The cache lives in memory only and is cleared when the process dies.
 *
 * It is **off by default**. Turn it on once for the whole app, for example in
 * `Application.onCreate()`:
 *
 * ```
 * RichLinkCache.enabled = true
 * RichLinkCache.maxSize = 200            // optional, default 100
 * RichLinkCache.expireAfter = 30.minutes // optional, default 1 hour
 * ```
 */
public object RichLinkCache {

    private const val DEFAULT_MAX_SIZE = 100
    private val DEFAULT_EXPIRE_AFTER = 1.hours

    private class Entry(val metaData: MetaData, val storedAtMillis: Long)

    private val lock = Any()
    private var maxEntries = DEFAULT_MAX_SIZE
    private var isEnabled = false
    private var expiry: Duration = DEFAULT_EXPIRE_AFTER

    /** Monotonic clock in milliseconds. Internal so tests can control time. */
    internal var clock: () -> Long = { System.nanoTime() / 1_000_000 }

    // accessOrder = true: iteration order is least recently used first.
    private val entries = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean =
            size > maxEntries
    }

    /**
     * Whether previews read from and write to the cache. Off by default. While disabled,
     * [get] returns `null` and [put] does nothing. Turning it off also clears the cache.
     */
    @JvmStatic
    public var enabled: Boolean
        get() = synchronized(lock) { isEnabled }
        set(value) {
            synchronized(lock) {
                isEnabled = value
                if (!value) entries.clear()
            }
        }

    /** Maximum number of URLs kept. Lowering it drops the least recently used entries; 0 disables caching. */
    @JvmStatic
    public var maxSize: Int
        get() = synchronized(lock) { maxEntries }
        set(value) {
            require(value >= 0) { "maxSize must be >= 0, was $value" }
            synchronized(lock) {
                maxEntries = value
                val iterator = entries.entries.iterator()
                while (entries.size > maxEntries && iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }

    /**
     * How long an entry stays valid after it is stored. After that it counts as missing and
     * the next preview fetches the page again. Default 1 hour; [Duration.INFINITE] never expires.
     */
    @JvmStatic
    public var expireAfter: Duration
        get() = synchronized(lock) { expiry }
        set(value) {
            require(value.isPositive()) { "expireAfter must be positive, was $value" }
            synchronized(lock) { expiry = value }
        }

    /**
     * Java-friendly way to set [expireAfter], for example
     * `RichLinkCache.setExpireAfter(30, TimeUnit.MINUTES)`. [Long.MAX_VALUE] never expires.
     */
    @JvmStatic
    public fun setExpireAfter(duration: Long, unit: TimeUnit) {
        require(duration > 0) { "expireAfter must be positive, was $duration $unit" }
        expireAfter = if (duration == Long.MAX_VALUE) Duration.INFINITE else unit.toMillis(duration).milliseconds
    }

    /** Java-friendly read of [expireAfter] in milliseconds; [Long.MAX_VALUE] means it never expires. */
    @JvmStatic
    public fun getExpireAfterMillis(): Long = expireAfter.let { if (it.isInfinite()) Long.MAX_VALUE else it.inWholeMilliseconds }

    /** Number of URLs currently stored, including entries that have expired but were not read since. */
    @JvmStatic
    public val size: Int
        get() = synchronized(lock) { entries.size }

    /**
     * Returns the cached metadata for [url], or `null` if it is not cached, has expired,
     * or the cache is disabled.
     */
    @JvmOverloads
    @JvmStatic
    public fun get(url: String, userAgent: String? = null): MetaData? = synchronized(lock) {
        if (!isEnabled) return null
        val key = key(url, userAgent)
        val entry = entries[key] ?: return null
        if (isExpired(entry)) {
            entries.remove(key)
            null
        } else {
            entry.metaData
        }
    }

    /**
     * Stores [metaData] for [url], for example to pre-fill previews from your own backend.
     * Does nothing while the cache is disabled.
     */
    @JvmOverloads
    @JvmStatic
    public fun put(url: String, metaData: MetaData, userAgent: String? = null) {
        synchronized(lock) {
            if (isEnabled && maxEntries > 0) entries[key(url, userAgent)] = Entry(metaData, clock())
        }
    }

    /** Removes [url] so its next preview is fetched again. */
    @JvmOverloads
    @JvmStatic
    public fun remove(url: String, userAgent: String? = null) {
        synchronized(lock) { entries.remove(key(url, userAgent)) }
    }

    /** Removes every cached URL. */
    @JvmStatic
    public fun clear() {
        synchronized(lock) { entries.clear() }
    }

    private fun isExpired(entry: Entry): Boolean =
        !expiry.isInfinite() && clock() - entry.storedAtMillis >= expiry.inWholeMilliseconds

    // A custom user agent can get a different page, so it is part of the key.
    internal fun key(url: String, userAgent: String?): String =
        if (userAgent.isNullOrBlank()) url else "$url\u0000$userAgent"
}
