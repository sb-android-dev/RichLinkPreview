package com.sbdev.project.richlinkpreview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colours used by the built-in preview cards.
 *
 * The Skype card draws its text over the preview image, so it always uses white text
 * and only takes [container] (shown when there is no image) from here.
 */
@Immutable
public data class RichLinkColors(
    val container: Color,
    val title: Color,
    val description: Color,
    val url: Color,
    val accent: Color
)

public object RichLinkDefaults {
    /** Default colours, derived from the current [MaterialTheme] so dark mode works. */
    @Composable
    public fun colors(
        container: Color = MaterialTheme.colorScheme.surfaceContainerLow,
        title: Color = MaterialTheme.colorScheme.onSurface,
        description: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        url: Color = MaterialTheme.colorScheme.outline,
        accent: Color = MaterialTheme.colorScheme.primary
    ): RichLinkColors = RichLinkColors(
        container = container,
        title = title,
        description = description,
        url = url,
        accent = accent
    )
}
