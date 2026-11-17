package com.sbdev.project.richlinkpreview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage

/**
 * Calls [onClicked] when the card is tapped, or opens [MetaData.originalUrl] in the
 * browser when no handler is given.
 */
internal fun Modifier.richLinkClick(
    metaData: MetaData,
    onClicked: ((MetaData) -> Unit)?
): Modifier = composed {
    val context = LocalContext.current
    clickable {
        if (onClicked != null) {
            onClicked(metaData)
        } else {
            val link = metaData.originalUrl.ifBlank { metaData.url }
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
            } catch (_: ActivityNotFoundException) {
                // No app can open this link; nothing sensible to do.
            }
        }
    }
}

/** Horizontal card with the image on the left, then title, description and URL. */
@Composable
public fun DefaultRichLinkCard(
    metaData: MetaData,
    modifier: Modifier = Modifier,
    colors: RichLinkColors = RichLinkDefaults.colors(),
    onClicked: ((MetaData) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clip(RoundedCornerShape(8.dp))
            .richLinkClick(metaData, onClicked),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colors.container)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            PreviewImage(metaData.imageUrl, shape = RoundedCornerShape(4.dp))
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            ) {
                TitleText(metaData.title, colors.title, maxLines = 1)
                DescriptionText(metaData.description, colors.description)
                if (metaData.url.isNotBlank()) {
                    Text(
                        text = metaData.url,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.url,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/** Soft shadow that keeps white text readable over bright parts of an image. */
private val OverlayTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.6f),
    offset = Offset(0f, 1f),
    blurRadius = 4f
)

/** Large card with the image as background and white text over a gradient. */
@Composable
public fun SkypeRichLinkCard(
    metaData: MetaData,
    modifier: Modifier = Modifier,
    colors: RichLinkColors = RichLinkDefaults.colors(),
    onClicked: ((MetaData) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(5.dp)
            .clip(RoundedCornerShape(12.dp))
            .richLinkClick(metaData, onClicked),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colors.container)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (metaData.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = metaData.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Scrim in fractions of the card height (not pixels), so it lines up with
                    // the text on every screen density. The top third stays clear to show the
                    // image; the text starts at ~40%, so the scrim ramps up just before it.
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.3f to Color.Transparent,
                            0.5f to Color.Black.copy(alpha = 0.6f),
                            0.65f to Color.Black.copy(alpha = 0.8f),
                            1f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                TitleText(metaData.title, Color.White, maxLines = 1, fontSize = 16, shadow = OverlayTextShadow)
                if (metaData.description.isNotBlank()) {
                    Text(
                        text = metaData.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, shadow = OverlayTextShadow),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (metaData.favicon.isNotBlank()) {
                        AsyncImage(
                            model = metaData.favicon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = metaData.url,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, shadow = OverlayTextShadow),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Compact card with the image on the left, title and description. */
@Composable
public fun TwitterRichLinkCard(
    metaData: MetaData,
    modifier: Modifier = Modifier,
    colors: RichLinkColors = RichLinkDefaults.colors(),
    onClicked: ((MetaData) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clip(RoundedCornerShape(12.dp))
            .richLinkClick(metaData, onClicked),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colors.container)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            PreviewImage(metaData.imageUrl, shape = RoundedCornerShape(4.dp))
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            ) {
                TitleText(metaData.title, colors.title, maxLines = 1)
                DescriptionText(metaData.description, colors.description)
            }
        }
    }
}

/** The link as underlined text, followed by a quote block with an accent bar. */
@Composable
public fun TelegramRichLinkCard(
    metaData: MetaData,
    modifier: Modifier = Modifier,
    colors: RichLinkColors = RichLinkDefaults.colors(),
    onClicked: ((MetaData) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clip(RoundedCornerShape(12.dp))
            .richLinkClick(metaData, onClicked)
            .background(colors.container)
            .padding(10.dp)
    ) {
        Text(
            text = metaData.originalUrl,
            color = colors.accent,
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.accent.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(90.dp)
                    .background(colors.accent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
            ) {
                if (metaData.url.isNotBlank()) {
                    Text(
                        text = metaData.url,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = colors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TitleText(metaData.title, colors.title, maxLines = 2)
                DescriptionText(metaData.description, colors.description)
            }

            PreviewImage(metaData.imageUrl, shape = RoundedCornerShape(0.dp))
        }
    }
}

/**
 * Thumbnail for the side-by-side cards. Uses [ContentScale.Fit] so the whole image is
 * visible: og:images are usually wide (about 1.91:1) and often logos or text, which a
 * crop into this near-square box would cut off.
 */
@Composable
private fun PreviewImage(imageUrl: String, shape: RoundedCornerShape) {
    if (imageUrl.isNotBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(width = 110.dp, height = 90.dp)
                .clip(shape),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun TitleText(
    title: String,
    color: Color,
    maxLines: Int,
    fontSize: Int = 14,
    shadow: Shadow? = null
) {
    if (title.isNotBlank()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = fontSize.sp,
                shadow = shadow
            ),
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DescriptionText(description: String, color: Color) {
    if (description.isNotBlank()) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private val sampleMetaData = MetaData(
    title = "GitHub: Let's build from here",
    description = "GitHub is where over 100 million developers shape the future of software, together.",
    url = "github.com",
    originalUrl = "https://github.com",
    imageUrl = "https://github.githubassets.com/images/modules/open_graph/github-logo.png",
    favicon = "https://github.githubassets.com/favicons/favicon.svg"
)

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    MaterialTheme { DefaultRichLinkCard(sampleMetaData) }
}

@Preview(showBackground = true)
@Composable
private fun SkypePreview() {
    MaterialTheme { SkypeRichLinkCard(sampleMetaData) }
}

@Preview(showBackground = true)
@Composable
private fun TwitterPreview() {
    MaterialTheme { TwitterRichLinkCard(sampleMetaData) }
}

@Preview(showBackground = true)
@Composable
private fun TelegramPreview() {
    MaterialTheme { TelegramRichLinkCard(sampleMetaData) }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            Column {
                DefaultRichLinkCard(sampleMetaData)
                TwitterRichLinkCard(sampleMetaData)
                TelegramRichLinkCard(sampleMetaData)
            }
        }
    }
}
