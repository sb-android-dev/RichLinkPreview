package com.sbdev.project.richlinkpreview

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test

class RichPreviewParseTest {

    private val pageUrl = "https://example.com/blog/post"

    private fun parse(head: String) =
        RichPreview.parse(Jsoup.parse("<html><head>$head</head><body></body></html>"), pageUrl)

    @Test
    fun `uses open graph tags when present`() {
        val meta = parse(
            """
            <title>Plain title</title>
            <meta property="og:title" content="OG title">
            <meta property="og:description" content="OG description">
            <meta property="og:image" content="https://cdn.example.com/a.png">
            <meta property="og:url" content="https://example.com/canonical">
            <meta property="og:site_name" content="Example">
            """
        )
        assertEquals("OG title", meta.title)
        assertEquals("OG description", meta.description)
        assertEquals("https://cdn.example.com/a.png", meta.imageUrl)
        assertEquals("https://example.com/canonical", meta.url)
        assertEquals("Example", meta.siteName)
        assertEquals(pageUrl, meta.originalUrl)
    }

    @Test
    fun `falls back to title tag and meta description`() {
        val meta = parse(
            """
            <title>Plain title</title>
            <meta name="description" content="Meta description">
            <meta property="og:description" content="OG description">
            """
        )
        assertEquals("Plain title", meta.title)
        assertEquals("Meta description", meta.description)
    }

    @Test
    fun `resolves relative image and favicon against the page url`() {
        val meta = parse(
            """
            <meta property="og:image" content="/img/cover.png">
            <link rel="icon" href="favicon.ico">
            """
        )
        assertEquals("https://example.com/img/cover.png", meta.imageUrl)
        assertEquals("https://example.com/blog/favicon.ico", meta.favicon)
    }

    @Test
    fun `uses the favicon as image when there is no og image`() {
        val meta = parse("""<link rel="apple-touch-icon" href="/touch.png">""")
        assertEquals("https://example.com/touch.png", meta.imageUrl)
        assertEquals("https://example.com/touch.png", meta.favicon)
    }

    @Test
    fun `falls back to the host when og url is missing`() {
        assertEquals("example.com", parse("").url)
    }
}
