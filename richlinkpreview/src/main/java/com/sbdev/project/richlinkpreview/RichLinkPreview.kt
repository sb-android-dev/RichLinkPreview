package com.sbdev.project.richlinkpreview

import android.content.Intent
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

enum class RichLinkStyle {
    DEFAULT, SKYPE, TWITTER, TELEGRAM
}

@Composable
fun RichLinkPreview(
    url: String,
    modifier: Modifier = Modifier,
    style: RichLinkStyle = RichLinkStyle.DEFAULT,
    onClicked: ((MetaData) -> Unit)? = null
) {
    var metaData by remember { mutableStateOf<MetaData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val richPreview = remember { RichPreview() }

    LaunchedEffect(url) {
        isLoading = true
        metaData = richPreview.getPreviewSuspend(url)
        isLoading = false
    }

    if (isLoading) {
        // Optional: Add loading placeholder
    } else if (metaData != null) {
        val data = metaData!!
        when (style) {
            RichLinkStyle.DEFAULT -> DefaultRichLinkCard(data, modifier, onClicked)
            RichLinkStyle.SKYPE -> SkypeRichLinkCard(data, modifier, onClicked)
            RichLinkStyle.TWITTER -> TwitterRichLinkCard(data, modifier, onClicked)
            RichLinkStyle.TELEGRAM -> TelegramRichLinkCard(data, modifier, onClicked)
        }
    }
}

@Composable
private fun DefaultRichLinkCard(
    data: MetaData,
    modifier: Modifier = Modifier,
    onClicked: ((MetaData) -> Unit)? = null
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clickable {
                if (onClicked != null) {
                    onClicked(data)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, data.originalUrl.toUri())
                    context.startActivity(intent)
                }
            },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            if (data.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 110.dp, height = 90.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            ) {
                if (data.title.isNotBlank()) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF212121)
                    )
                }
                if (data.description.isNotBlank()) {
                    Text(
                        text = data.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF90A4AE),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (data.url.isNotBlank()) {
                    Text(
                        text = data.url,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF455A64),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SkypeRichLinkCard(
    data: MetaData,
    modifier: Modifier = Modifier,
    onClicked: ((MetaData) -> Unit)? = null
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(5.dp)
            .clickable {
                if (onClicked != null) {
                    onClicked(data)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, data.originalUrl.toUri())
                    context.startActivity(intent)
                }
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (data.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                if (data.title.isNotBlank()) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (data.description.isNotBlank()) {
                    Text(
                        text = data.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = Color.White
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (data.favicon.isNotBlank()) {
                        AsyncImage(
                            model = data.favicon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = data.url,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TwitterRichLinkCard(
    data: MetaData,
    modifier: Modifier = Modifier,
    onClicked: ((MetaData) -> Unit)? = null
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clickable {
                if (onClicked != null) {
                    onClicked(data)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, data.originalUrl.toUri())
                    context.startActivity(intent)
                }
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            if (data.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 110.dp, height = 90.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            ) {
                if (data.title.isNotBlank()) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF212121)
                    )
                }
                if (data.description.isNotBlank()) {
                    Text(
                        text = data.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF90A4AE),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TelegramRichLinkCard(
    data: MetaData,
    modifier: Modifier = Modifier,
    onClicked: ((MetaData) -> Unit)? = null
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clickable {
                if (onClicked != null) {
                    onClicked(data)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, data.originalUrl.toUri())
                    context.startActivity(intent)
                }
            }
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = data.originalUrl,
            color = Color(0xFF29B6F6),
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                .padding(start = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(90.dp)
                    .background(Color(0xFF039BE5))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
            ) {
                if (data.url.isNotBlank()) {
                    Text(
                        text = data.url,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF039BE5)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (data.title.isNotBlank()) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF212121)
                    )
                }
                if (data.description.isNotBlank()) {
                    Text(
                        text = data.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF90A4AE),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (data.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 110.dp, height = 90.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    DefaultRichLinkCard(
        data = MetaData(
            title = "Google",
            description = "Search the world's information, including webpages, images, videos and more.",
            url = "google.com",
            imageUrl = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun SkypePreview() {
    SkypeRichLinkCard(
        data = MetaData(
            title = "GitHub: Let’s build from here",
            description = "GitHub is where over 100 million developers shape the future of software, together.",
            url = "github.com",
            imageUrl = "https://github.githubassets.com/images/modules/open_graph/github-logo.png",
            favicon = "https://github.githubassets.com/favicons/favicon.svg"
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun TwitterPreview() {
    TwitterRichLinkCard(
        data = MetaData(
            title = "X / Twitter",
            description = "From breaking news and entertainment to sports and politics, get the full story with all the live commentary.",
            url = "twitter.com",
            imageUrl = "https://abs.twimg.com/errors/logo46x38.png"
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun TelegramPreview() {
    TelegramRichLinkCard(
        data = MetaData(
            title = "Telegram Messenger",
            description = "Telegram is a cloud-based mobile and desktop messaging app with a focus on security and speed.",
            url = "telegram.org",
            originalUrl = "https://telegram.org",
            imageUrl = "https://telegram.org/img/t_logo.png"
        )
    )
}
