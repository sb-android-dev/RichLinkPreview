package com.sbdev.project.richlinkpreview

import androidx.compose.runtime.Immutable

/**
 * Metadata extracted from a web page. Empty strings mean "not found".
 *
 * @property url Canonical URL from `og:url`, falling back to the host of [originalUrl].
 * @property originalUrl The URL that was requested.
 */
@Immutable
public data class MetaData(
    val url: String = "",
    val imageUrl: String = "",
    val title: String = "",
    val description: String = "",
    val siteName: String = "",
    val mediaType: String = "",
    val favicon: String = "",
    val originalUrl: String = ""
)
