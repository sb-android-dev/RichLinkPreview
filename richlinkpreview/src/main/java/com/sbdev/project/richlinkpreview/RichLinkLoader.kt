package com.sbdev.project.richlinkpreview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Loads metadata for previews: from [RichLinkCache] when possible, otherwise from the
 * network, with at most one fetch running per URL at a time.
 *
 * When several previews ask for the same URL at once (the same link in a list, or a chat
 * and its detail screen), they all wait for that single fetch. The fetch runs in the
 * loader's own scope, so a preview that leaves the screen stops waiting without cancelling
 * the fetch for the others. The result is cached before the fetch is marked finished, so a
 * preview arriving right after it completes reads the cache instead of starting a new fetch.
 *
 * Every successful fetch is also published on [updates], so previews already on screen
 * pick up the result of a [refresh].
 */
internal class RichLinkLoader(
    private val cache: RichLinkCache = RichLinkCache,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val fetcher: suspend (url: String, userAgent: String?) -> Result<MetaData> = { url, userAgent ->
        RichPreview().also { it.userAgent = userAgent }.fetch(url)
    }
) {
    private val lock = Any()
    private val inFlight = HashMap<String, Deferred<Result<MetaData>>>()

    private class Update(val key: String, val metaData: MetaData)

    // No replay: a preview only needs updates that happen while it is on screen.
    private val updateFlow = MutableSharedFlow<Update>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Loads [url], using the cache when it has a valid entry. */
    suspend fun load(url: String, userAgent: String? = null): Result<MetaData> {
        cache.get(url, userAgent)?.let { return Result.success(it) }
        return sharedFetch(url, userAgent).await()
    }

    /**
     * Fetches [url] again, ignoring the cache. Joins a fetch that is already running, since
     * that one is fresh too. On success the cache is updated and previews showing [url] are
     * notified through [updates]; on failure they keep what they show.
     */
    suspend fun refresh(url: String, userAgent: String? = null): Result<MetaData> =
        sharedFetch(url, userAgent).await()

    /** Successful fetches of [url], for previews that are already showing it. */
    fun updates(url: String, userAgent: String? = null): Flow<MetaData> {
        val key = cache.key(url, userAgent)
        return updateFlow.filter { it.key == key }.map { it.metaData }
    }

    private fun sharedFetch(url: String, userAgent: String?): Deferred<Result<MetaData>> {
        val key = cache.key(url, userAgent)
        synchronized(lock) {
            inFlight[key]?.let { return it }
            val fetch = scope.async {
                fetcher(url, userAgent).onSuccess {
                    cache.put(url, it, userAgent)
                    updateFlow.tryEmit(Update(key, it))
                }
            }
            inFlight[key] = fetch
            fetch.invokeOnCompletion {
                synchronized(lock) {
                    if (inFlight[key] === fetch) inFlight.remove(key)
                }
            }
            return fetch
        }
    }

    /** Number of fetches currently running. For tests. */
    internal val inFlightCount: Int
        get() = synchronized(lock) { inFlight.size }

    internal companion object {
        val Default = RichLinkLoader()
    }
}
