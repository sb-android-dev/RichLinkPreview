package com.sbdev.project.richlinkpreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.launch

/** Loading state of a link preview. */
@Immutable
public sealed interface RichLinkState {
    public data object Loading : RichLinkState
    public data class Success(val metaData: MetaData) : RichLinkState
    public data class Error(val exception: Throwable) : RichLinkState
}

/**
 * Fetches the metadata for [url] and exposes it as state. Use this to build a
 * completely custom preview, including your own loading and error UI.
 *
 * The fetch restarts when [url] or [userAgent] changes and is cancelled when the
 * composable leaves the composition.
 *
 * Previews of the same URL that load at the same time share a single network request.
 * When [RichLinkCache.enabled] is `true`, a URL that is already cached (and not expired)
 * starts in [RichLinkState.Success] without a network request, and each successful fetch
 * is cached.
 *
 * The state also follows [RichLinks.refresh]: when [url] is refreshed successfully, it
 * switches to the new metadata without passing through [RichLinkState.Loading].
 */
@Composable
public fun rememberRichLinkState(url: String, userAgent: String? = null): State<RichLinkState> {
    // Read synchronously so a cached preview is shown on the first frame, with no Loading flash.
    // RichLinkCache.get returns null while the cache is disabled.
    fun cachedState(): RichLinkState.Success? =
        RichLinkCache.get(url, userAgent)?.let { RichLinkState.Success(it) }

    return produceState(cachedState() ?: RichLinkState.Loading, url, userAgent) {
        val loader = RichLinkLoader.Default
        // Subscribe before loading so a refresh that finishes meanwhile is not missed.
        launch {
            loader.updates(url, userAgent).collect { value = RichLinkState.Success(it) }
        }

        val cached = cachedState()
        if (cached != null) {
            value = cached
            return@produceState
        }
        value = RichLinkState.Loading
        // The loader shares one fetch between previews of the same URL and caches the result.
        val result = loader.load(url, userAgent)
        // A refresh may already have delivered newer data; only fill in what is still missing.
        if (value !is RichLinkState.Success || result.isSuccess) {
            value = result.fold(
                onSuccess = { RichLinkState.Success(it) },
                onFailure = { RichLinkState.Error(it) }
            )
        }
    }
}
