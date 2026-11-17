package com.sbdev.project.richlinkpreview

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/** The built-in card designs. For your own design, use the `content` overload of [RichLinkPreview]. */
public enum class RichLinkStyle {
    DEFAULT, SKYPE, TWITTER, TELEGRAM
}

/**
 * Fetches [url] and shows it with one of the built-in card [style]s.
 *
 * @param onClicked called when the card is tapped. When `null`, the link opens in the browser.
 * @param loading shown while the page is being fetched. Shows nothing by default.
 * @param error shown when the page could not be loaded. Shows nothing by default.
 */
// `colors` is deliberately last: if a lambda parameter were last, a trailing lambda
// `RichLinkPreview(url) { meta -> ... }` would be ambiguous with the `content` overload.
@Composable
public fun RichLinkPreview(
    url: String,
    modifier: Modifier = Modifier,
    style: RichLinkStyle = RichLinkStyle.DEFAULT,
    onClicked: ((MetaData) -> Unit)? = null,
    loading: @Composable () -> Unit = {},
    error: @Composable (Throwable) -> Unit = {},
    colors: RichLinkColors = RichLinkDefaults.colors()
) {
    RichLinkPreview(url = url, modifier = modifier, loading = loading, error = error) { metaData ->
        RichLinkCard(metaData = metaData, style = style, colors = colors, onClicked = onClicked)
    }
}

/**
 * Fetches [url] and renders the result with your own [content].
 *
 * ```
 * RichLinkPreview(url) { metaData ->
 *     MyCard(metaData.title, metaData.imageUrl)
 * }
 * ```
 *
 * For full control over every state, use [rememberRichLinkState] instead.
 */
@Composable
public fun RichLinkPreview(
    url: String,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit = {},
    error: @Composable (Throwable) -> Unit = {},
    content: @Composable (MetaData) -> Unit
) {
    val state by rememberRichLinkState(url)
    Box(modifier) {
        when (val current = state) {
            RichLinkState.Loading -> loading()
            is RichLinkState.Error -> error(current.exception)
            is RichLinkState.Success -> content(current.metaData)
        }
    }
}

/** Renders already-fetched [metaData] with a built-in [style]. */
@Composable
public fun RichLinkCard(
    metaData: MetaData,
    modifier: Modifier = Modifier,
    style: RichLinkStyle = RichLinkStyle.DEFAULT,
    colors: RichLinkColors = RichLinkDefaults.colors(),
    onClicked: ((MetaData) -> Unit)? = null
) {
    when (style) {
        RichLinkStyle.DEFAULT -> DefaultRichLinkCard(metaData, modifier, colors, onClicked)
        RichLinkStyle.SKYPE -> SkypeRichLinkCard(metaData, modifier, colors, onClicked)
        RichLinkStyle.TWITTER -> TwitterRichLinkCard(metaData, modifier, colors, onClicked)
        RichLinkStyle.TELEGRAM -> TelegramRichLinkCard(metaData, modifier, colors, onClicked)
    }
}
