package com.sbdev.project.richlinkpreview

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI

/**
 * Fetches a page with Jsoup and extracts its [MetaData].
 *
 * Compose callers should prefer [rememberRichLinkState] or [RichLinkPreview].
 */
public class RichPreview(
    private val responseListener: ResponseListener? = null
) {
    public var userAgent: String? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Callback version for non-coroutine callers. The result is delivered to the
     * [ResponseListener] passed to the constructor, on the main thread.
     */
    public fun getPreview(url: String) {
        scope.launch {
            getPreviewSuspend(url)
        }
    }

    public fun getPreview(url: String, userAgent: String) {
        this.userAgent = userAgent
        getPreview(url)
    }

    /**
     * Suspend version. Returns the metadata, or `null` if the page could not be loaded.
     * Also notifies the optional [ResponseListener].
     */
    public suspend fun getPreviewSuspend(url: String): MetaData? {
        val result = fetch(url)
        withContext(Dispatchers.Main) {
            result.fold(
                onSuccess = { responseListener?.onData(it) },
                onFailure = {
                    responseListener?.onError(
                        Exception("Could not load preview for $url: ${it.localizedMessage}", it)
                    )
                }
            )
        }
        return result.getOrNull()
    }

    internal suspend fun fetch(url: String): Result<MetaData> = withContext(Dispatchers.IO) {
        try {
            var connection = Jsoup.connect(url).timeout(TIMEOUT_MS)
            userAgent?.takeIf { it.isNotBlank() }?.let { connection = connection.userAgent(it) }
            Result.success(parse(connection.get(), url))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal companion object {
        private const val TIMEOUT_MS = 30_000

        /** Extracts [MetaData] from an already-loaded document. Pure, so it is unit-testable. */
        fun parse(doc: Document, url: String): MetaData {
            fun meta(selector: String) = doc.select(selector).attr("content")
            fun link(rel: String) = doc.select("link[rel=$rel]").attr("href")

            val title = meta("meta[property=og:title]").ifBlank { doc.title() }

            val description = meta("meta[name=description]")
                .ifBlank { meta("meta[name=Description]") }
                .ifBlank { meta("meta[property=og:description]") }

            val touchIcon = link("apple-touch-icon")
            val icon = link("icon")
            val faviconSrc = touchIcon.ifEmpty { icon }
            val favicon = if (faviconSrc.isNotEmpty()) resolveURL(url, faviconSrc) else ""

            val imageSrc = meta("meta[property=og:image]")
                .ifEmpty { meta("meta[name=og:image]") }
                .ifEmpty { link("image_src") }
                .ifEmpty { faviconSrc }
            val imageUrl = if (imageSrc.isNotEmpty()) resolveURL(url, imageSrc) else ""

            var canonicalUrl = ""
            var siteName = ""
            for (element in doc.getElementsByTag("meta")) {
                when (element.attr("property").trim()) {
                    "og:url" -> canonicalUrl = element.attr("content")
                    "og:site_name" -> siteName = element.attr("content")
                }
            }
            if (canonicalUrl.isBlank()) {
                canonicalUrl = runCatching { URI(url).host }.getOrNull() ?: url
            }

            return MetaData(
                url = canonicalUrl,
                imageUrl = imageUrl,
                title = title,
                description = description,
                siteName = siteName,
                favicon = favicon,
                originalUrl = url
            )
        }

        private fun resolveURL(url: String, part: String): String {
            val partUri = runCatching { URI(part) }.getOrNull()
            if (partUri?.isAbsolute == true) return part
            return runCatching { URI(url).resolve(part).toString() }.getOrDefault("")
        }
    }
}
