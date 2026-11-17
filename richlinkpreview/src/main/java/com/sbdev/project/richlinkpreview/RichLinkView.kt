package com.sbdev.project.richlinkpreview

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

/**
 * A View for XML layouts that shows the same Compose cards as [RichLinkPreview].
 * The app using it does not need to enable Compose itself.
 *
 * ```xml
 * <com.sbdev.project.richlinkpreview.RichLinkView
 *     android:id="@+id/richLinkView"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:richLinkStyle="skype" />
 * ```
 */
public class RichLinkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private var url by mutableStateOf<String?>(null)
    private var metaData by mutableStateOf<MetaData?>(null)
    private var linkStyle by mutableStateOf(RichLinkStyle.DEFAULT)
    private var viewListener by mutableStateOf<ViewListener?>(null)
    private var richLinkListener by mutableStateOf<RichLinkListener?>(null)
    private var isDefaultClick by mutableStateOf(false)

    init {
        context.obtainStyledAttributes(attrs, R.styleable.RichLinkView, defStyleAttr, 0).apply {
            try {
                val index = getInt(R.styleable.RichLinkView_richLinkStyle, 0)
                linkStyle = RichLinkStyle.entries.getOrElse(index) { RichLinkStyle.DEFAULT }
            } finally {
                recycle()
            }
        }
    }

    /** Fetches [url] and shows it with [style]. */
    @JvmOverloads
    public fun setLink(url: String, style: RichLinkStyle = linkStyle) {
        linkStyle = style
        this.viewListener = null
        this.metaData = null
        this.url = url
    }

    /**
     * Fetches [url] and reports the result to [viewListener]. `onSuccess(true)` is called
     * when the page has a title; `onError` is called when it could not be loaded.
     */
    public fun setLink(url: String, viewListener: ViewListener) {
        this.viewListener = viewListener
        this.metaData = null
        this.url = url
    }

    /** Shows already-fetched [metaData] without a network request. */
    public fun setLinkFromMeta(metaData: MetaData) {
        this.url = null
        this.metaData = metaData
    }

    public fun setStyle(style: RichLinkStyle) {
        linkStyle = style
    }

    /** Handles card taps. Without a listener, the link opens in the browser. */
    public fun setClickListener(richLinkListener: RichLinkListener) {
        this.richLinkListener = richLinkListener
    }

    /** When `true`, taps always open the browser, even if a [RichLinkListener] is set. */
    public fun setDefaultClickListener(isDefault: Boolean) {
        this.isDefaultClick = isDefault
    }

    @Composable
    override fun Content() {
        val listener = richLinkListener
        val onClicked: ((MetaData) -> Unit)? =
            if (isDefaultClick || listener == null) null
            else { meta -> listener.onClicked(this, meta) }

        val fixedMeta = metaData
        val currentUrl = url
        when {
            fixedMeta != null -> RichLinkCard(fixedMeta, style = linkStyle, onClicked = onClicked)
            currentUrl != null -> {
                val state by rememberRichLinkState(currentUrl)
                val current = state
                LaunchedEffect(current) {
                    when (current) {
                        is RichLinkState.Success ->
                            if (current.metaData.title.isNotBlank()) viewListener?.onSuccess(true)
                        is RichLinkState.Error -> viewListener?.onError(
                            current.exception as? Exception ?: Exception(current.exception)
                        )
                        RichLinkState.Loading -> Unit
                    }
                }
                if (current is RichLinkState.Success) {
                    RichLinkCard(current.metaData, style = linkStyle, onClicked = onClicked)
                }
            }
        }
    }
}
