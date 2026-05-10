package com.sbdev.project.richlinkpreview

import android.webkit.URLUtil
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

class RichPreview(
    private val responseListener: ResponseListener? = null
) {
    var userAgent: String? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Non-suspend version for legacy View-based usage.
     * Uses the ResponseListener provided in the constructor.
     */
    fun getPreview(url: String) {
        scope.launch {
            getPreviewSuspend(url)
        }
    }

    fun getPreview(url: String, userAgent: String) {
        this.userAgent = userAgent
        getPreview(url)
    }

    /**
     * Suspend version for Compose/Coroutines usage.
     * Returns MetaData directly.
     */
    suspend fun getPreviewSuspend(url: String): MetaData? = withContext(Dispatchers.IO) {
        val metaData = MetaData()
        metaData.originalUrl = url
        try {
            var connection = Jsoup.connect(url).timeout(30 * 1000)
            userAgent?.let {
                if (it.isNotBlank()) {
                    connection = connection.userAgent(it)
                }
            }
            val doc = connection.get()

            var title = doc.select("meta[property=og:title]").attr("content")
            if (title.isBlank()) {
                title = doc.title()
            }
            metaData.title = title

            var description = doc.select("meta[name=description]").attr("content")
            if (description.isBlank())
                description = doc.select("meta[name=Description]").attr("content")
            if (description.isBlank())
                description = doc.select("meta[property=og:description]").attr("content")
            metaData.description = description

            val imageElements = doc.select("meta[property=og:image]")
            if (imageElements.size > 0) {
                val image = imageElements.attr("content")
                if (image.isNotEmpty()) {
                    metaData.imageUrl = resolveURL(url, image)
                }
            }

            if (metaData.imageUrl.isBlank()) {
                val imageElement = doc.select("meta[name=og:image]")
                if (imageElement.size > 0) {
                    val image = imageElement.attr("content")
                    if (image.isNotEmpty()) {
                        metaData.imageUrl = resolveURL(url, image)
                    }
                }
            }

            if (metaData.imageUrl.isBlank()) {
                var src = doc.select("link[rel=image_src]").attr("href")
                if (src.isNotEmpty()) {
                    metaData.imageUrl = resolveURL(url, src)
                } else {
                    src = doc.select("link[rel=apple-touch-icon]").attr("href")
                    if (src.isNotEmpty()) {
                        metaData.imageUrl = resolveURL(url, src)
                        metaData.favicon = resolveURL(url, src)
                    } else {
                        src = doc.select("link[rel=icon]").attr("href")
                        if (src.isNotEmpty()) {
                            metaData.imageUrl = resolveURL(url, src)
                            metaData.favicon = resolveURL(url, src)
                        }
                    }
                }
            }

            var src = doc.select("link[rel=apple-touch-icon]").attr("href")
            if (src.isNotEmpty()) {
                metaData.favicon = resolveURL(url, src)
            } else {
                src = doc.select("link[rel=icon]").attr("href")
                if (src.isNotEmpty()) {
                    metaData.favicon = resolveURL(url, src)
                }
            }

            val elements = doc.getElementsByTag("meta")
            for (element in elements) {
                if (element.hasAttr("property")) {
                    val strProperty = element.attr("property").trim()
                    if (strProperty == "og:url") {
                        metaData.url = element.attr("content")
                    }
                    if (strProperty == "og:site_name") {
                        metaData.siteName = element.attr("content")
                    }
                }
            }

            if (metaData.url.isBlank()) {
                try {
                    val uri = URI(url)
                    metaData.url = uri.host ?: url
                } catch (e: URISyntaxException) {
                    metaData.url = url
                }
            }
            
            withContext(Dispatchers.Main) {
                responseListener?.onData(metaData)
            }
            metaData
        } catch (e: IOException) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                responseListener?.onError(Exception(
                    "No Html Received from $url Check your Internet ${e.localizedMessage}"
                ))
            }
            null
        }
    }

    private fun resolveURL(url: String, part: String): String {
        if (URLUtil.isValidUrl(part)) {
            return part
        } else {
            try {
                val baseUri = URI(url)
                return baseUri.resolve(part).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }
    }
}
