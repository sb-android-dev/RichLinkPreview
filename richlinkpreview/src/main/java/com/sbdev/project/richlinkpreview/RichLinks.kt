package com.sbdev.project.richlinkpreview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * App-wide actions on link previews.
 *
 * Use [refresh] for pull-to-refresh. It fetches the page again (ignoring [RichLinkCache]),
 * and every preview showing that URL, in Compose or in a [RichLinkView], switches to the
 * new data. Previews keep their current content while the refresh runs, and keep it if
 * the refresh fails.
 *
 * ```
 * PullToRefreshBox(
 *     isRefreshing = refreshing,
 *     onRefresh = {
 *         scope.launch {
 *             refreshing = true
 *             RichLinks.refresh(urlsOnScreen)
 *             refreshing = false
 *         }
 *     }
 * ) { ... }
 * ```
 */
public object RichLinks {

    private val callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Fetches [url] again and updates every preview showing it. Returns the new metadata or the error. */
    public suspend fun refresh(url: String, userAgent: String? = null): Result<MetaData> =
        RichLinkLoader.Default.refresh(url, userAgent)

    /**
     * Fetches all [urls] again, in parallel, and updates every preview showing them.
     * Returns the result for each URL.
     */
    public suspend fun refresh(urls: Collection<String>, userAgent: String? = null): Map<String, Result<MetaData>> =
        coroutineScope {
            urls.distinct()
                .map { url -> async { url to refresh(url, userAgent) } }
                .awaitAll()
                .toMap()
        }

    /**
     * Callback version of [refresh] for Java and View-based code (for example a
     * `SwipeRefreshLayout`). [listener] is called on the main thread with the URLs that
     * could not be refreshed; the list is empty when all succeeded.
     */
    @JvmStatic
    @JvmOverloads
    public fun refresh(urls: Collection<String>, listener: RefreshListener, userAgent: String? = null) {
        callbackScope.launch {
            val failed = refresh(urls, userAgent).filterValues { it.isFailure }.keys.toList()
            listener.onRefreshed(failed)
        }
    }

    /** Called when a callback-based [refresh] finishes. */
    public fun interface RefreshListener {
        public fun onRefreshed(failedUrls: List<String>)
    }
}
