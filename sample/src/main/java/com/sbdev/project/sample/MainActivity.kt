package com.sbdev.project.sample

import kotlinx.coroutines.launch
import com.sbdev.project.richlinkpreview.RichLinks
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.ExperimentalMaterial3Api
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.sbdev.project.richlinkpreview.MetaData
import com.sbdev.project.richlinkpreview.RichLinkDefaults
import com.sbdev.project.richlinkpreview.RichLinkPreview
import com.sbdev.project.richlinkpreview.RichLinkState
import com.sbdev.project.richlinkpreview.RichLinkStyle
import com.sbdev.project.richlinkpreview.RichLinkView
import com.sbdev.project.richlinkpreview.ViewListener
import com.sbdev.project.richlinkpreview.rememberRichLinkState
import com.sbdev.project.sample.ui.theme.RichLinkPreviewTheme

private const val DEMO_URL = "https://developer.android.com"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RichLinkPreviewTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    ShowcaseScreen(contentPadding = innerPadding, snackbarHostState = snackbarHostState)
                }
            }
        }
    }
}

/**
 * A plain scrolling Column, not a LazyColumn: a lazy list disposes sections that scroll
 * off screen, which would throw away their loaded previews and fetch them again. Here a
 * preview is only reloaded when its section is collapsed and expanded again, or on
 * pull-to-refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowcaseScreen(contentPadding: PaddingValues, snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                // Every preview of DEMO_URL on screen updates when this succeeds.
                val result = RichLinks.refresh(DEMO_URL)
                isRefreshing = false
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) "Previews refreshed" else "Couldn't refresh: ${result.exceptionOrNull()?.message}"
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Header()

            ShowcaseSection(
                title = "Built-in styles",
                description = "Four ready-made card designs, picked with the style parameter.",
                initiallyExpanded = true
            ) {
                Demo("Default", "RichLinkPreview(url)") {
                    RichLinkPreview(url = DEMO_URL)
                }
                Demo("Skype", "style = RichLinkStyle.SKYPE") {
                    RichLinkPreview(url = DEMO_URL, style = RichLinkStyle.SKYPE)
                }
                Demo("Twitter", "style = RichLinkStyle.TWITTER") {
                    RichLinkPreview(url = DEMO_URL, style = RichLinkStyle.TWITTER)
                }
                Demo("Telegram", "style = RichLinkStyle.TELEGRAM") {
                    RichLinkPreview(url = DEMO_URL, style = RichLinkStyle.TELEGRAM)
                }
            }

            ShowcaseSection(
                title = "Theming",
                description = "Cards follow MaterialTheme by default. Override any colour with RichLinkDefaults.colors()."
            ) {
                Demo("Custom accent", "colors = RichLinkDefaults.colors(accent = …)") {
                    RichLinkPreview(
                        url = DEMO_URL,
                        style = RichLinkStyle.TELEGRAM,
                        colors = RichLinkDefaults.colors(accent = Color(0xFF2E7D32))
                    )
                }
                Demo("Custom container and text", "colors = RichLinkDefaults.colors(container = …, title = …)") {
                    RichLinkPreview(
                        url = DEMO_URL,
                        colors = RichLinkDefaults.colors(
                            container = MaterialTheme.colorScheme.primaryContainer,
                            title = MaterialTheme.colorScheme.onPrimaryContainer,
                            description = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            url = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            ShowcaseSection(
                title = "Custom UI",
                description = "Bring your own design: render the fetched MetaData yourself."
            ) {
                Demo("Content slot", "RichLinkPreview(url) { metaData -> … }") {
                    RichLinkPreview(url = DEMO_URL) { metaData ->
                        CustomCard(metaData)
                    }
                }
                Demo("Every state yourself", "val state by rememberRichLinkState(url)") {
                    StateDemo(url = DEMO_URL)
                }
                Demo("Error state", "rememberRichLinkState(\"not a url\")") {
                    StateDemo(url = "not a url")
                }
            }

            ShowcaseSection(
                title = "XML Views",
                description = "RichLinkView shows the same cards in View-based layouts. Here it is hosted with AndroidView."
            ) {
                Demo("RichLinkView", "richLinkView.setLink(url, viewListener)") {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { context ->
                            RichLinkView(context).apply {
                                setStyle(RichLinkStyle.TELEGRAM)
                                setLink(DEMO_URL, object : ViewListener {
                                    override fun onSuccess(status: Boolean) {
                                        Toast.makeText(context, "RichLinkView loaded", Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onError(e: Exception) {
                                        Toast.makeText(context, "RichLinkView failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                })
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "RichLinkPreview",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Every way to show a link preview. Tap a section to expand or collapse it, and pull down to refresh.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** A collapsible group of demos with a header that toggles it. */
@Composable
private fun ShowcaseSection(
    title: String,
    description: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { expanded = !expanded }
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .padding(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = "▾",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .rotate(arrowRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                HorizontalDivider()
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/** One demo inside a section: a label, the code that produces it, then the result. */
@Composable
private fun Demo(label: String, code: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 6.dp)
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        content()
    }
}

/** Example of a fully custom design passed through the `content` slot. */
@Composable
private fun CustomCard(metaData: MetaData) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        if (metaData.imageUrl.isNotBlank()) {
            AsyncImage(
                model = metaData.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
        }
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (metaData.favicon.isNotBlank()) {
                    AsyncImage(
                        model = metaData.favicon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = metaData.siteName.ifBlank { metaData.url }.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = metaData.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (metaData.description.isNotBlank()) {
                Text(
                    text = metaData.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** Example of handling every state yourself with `rememberRichLinkState`. */
@Composable
private fun StateDemo(url: String) {
    val state by rememberRichLinkState(url)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        when (val current = state) {
            RichLinkState.Loading -> ListItem(
                headlineContent = { Text("Loading preview…") },
                leadingContent = { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
            )
            is RichLinkState.Error -> ListItem(
                headlineContent = { Text("Couldn't load preview") },
                supportingContent = {
                    Text(current.exception.message.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                leadingContent = {
                    Text("!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                },
                colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error)
            )
            is RichLinkState.Success -> ListItem(
                headlineContent = {
                    Text(current.metaData.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = { Text(current.metaData.url, maxLines = 1) },
                leadingContent = {
                    AsyncImage(
                        model = current.metaData.favicon.ifBlank { current.metaData.imageUrl },
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            )
        }
    }
}
