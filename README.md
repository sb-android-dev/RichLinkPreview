# RichLinkPreview

A lightweight Android library that generates rich link previews, built with **Jetpack Compose**. Use the built-in card designs, pass your own composable, or work with the fetched metadata directly. XML layouts are supported through a drop-in View.

## Features

- 🚀 **Compose first**: one composable for a ready-made preview, with public cards you can reuse.
- 🎨 **Four built-in styles**: `DEFAULT`, `SKYPE`, `TWITTER` and `TELEGRAM`, themable through `RichLinkColors` and dark-mode aware.
- 🧩 **Custom UI**: pass your own composable, or use `rememberRichLinkState` for full control over loading and error states.
- 📦 **Metadata extraction**: title, description, image, favicon and site name, fetched with Jsoup.
- 🔄 **Coroutine-based**: fetching runs off the main thread and is cancelled when the preview leaves the screen.
- 🛠️ **XML support**: `RichLinkView` shows the same Compose cards in View-based layouts, without enabling Compose in your app.

## Installation

RichLinkPreview is published on Maven Central, so no extra repository is needed. Add it to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.sb-android-dev:richlinkpreview:1.0.0")
}
```

Jsoup, Coil and kotlinx-coroutines come with the library automatically. Requires `minSdk` 24.

## Usage

### Built-in styles

```kotlin
RichLinkPreview(url = "https://www.google.com")

RichLinkPreview(
    url = "https://github.com",
    style = RichLinkStyle.SKYPE,
    onClicked = { metaData -> println("Clicked on: ${metaData.title}") }, // default: open in browser
    loading = { CircularProgressIndicator() },                           // default: nothing
    error = { e -> Text("Couldn't load preview") },                      // default: nothing
)
```

### Theming

Cards take their colours from `MaterialTheme` by default. Override any of them:

```kotlin
RichLinkPreview(
    url = "https://telegram.org",
    style = RichLinkStyle.TELEGRAM,
    colors = RichLinkDefaults.colors(accent = Color(0xFF039BE5))
)
```

`RichLinkColors` has `container`, `title`, `description`, `url` and `accent`. The Skype card always draws white text over its image.

### Custom UI

Pass a trailing lambda to render the fetched `MetaData` your own way:

```kotlin
RichLinkPreview(url = "https://kotlinlang.org") { metaData ->
    MyCard(title = metaData.title, image = metaData.imageUrl)
}
```

For full control over every state, use `rememberRichLinkState`:

```kotlin
val state by rememberRichLinkState(url)
when (val current = state) {
    RichLinkState.Loading -> Shimmer()
    is RichLinkState.Error -> Text("Failed: ${current.exception.message}")
    is RichLinkState.Success -> MyCard(current.metaData)
}
```

If you already have a `MetaData` (cached, or from your own backend), render a built-in card directly with `RichLinkCard(metaData, style = ...)`, or call `DefaultRichLinkCard`, `SkypeRichLinkCard`, `TwitterRichLinkCard` or `TelegramRichLinkCard`.

`MetaData` fields: `title`, `description`, `imageUrl`, `favicon`, `siteName`, `url` (canonical `og:url`, or the host) and `originalUrl` (the URL you requested). A missing value is an empty string.

### XML layouts

`RichLinkView` is a View that renders the same Compose cards. Your app does not need to enable Compose.

```xml
<com.sbdev.project.richlinkpreview.RichLinkView
    android:id="@+id/richLinkView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:richLinkStyle="skype" />
```

`app:richLinkStyle` accepts `defaultStyle`, `skype`, `twitter` or `telegram`.

```kotlin
val richLinkView = findViewById<RichLinkView>(R.id.richLinkView)

richLinkView.setLink("https://www.google.com")                    // or setLink(url, RichLinkStyle.TWITTER)

richLinkView.setLink("https://www.google.com", object : ViewListener {
    override fun onSuccess(status: Boolean) { }
    override fun onError(e: Exception) { }
})

richLinkView.setClickListener(object : RichLinkListener {
    override fun onClicked(view: View, metaData: MetaData) { }
})
```

The host must be an `AppCompatActivity`, a `ComponentActivity` or a Fragment.

### Caching

Previews in a `LazyColumn` or `RecyclerView` are recreated each time they scroll back into view, so each link would be fetched again. Turn on the in-memory cache once for the whole app, for example in `Application.onCreate()`:

```kotlin
RichLinkCache.enabled = true   // off by default
RichLinkCache.maxSize = 200    // optional, default 100 (least recently used entries are dropped)
RichLinkCache.expireAfter = 30.minutes // optional, default 1 hour
```

With the cache on, a URL that was already fetched is shown immediately, without a loading state. An entry older than `expireAfter` is fetched again, so a page that changed gets a fresh preview (use `Duration.INFINITE` to never expire). Only successful results are cached, so a failed URL is retried. You can also pre-fill or invalidate entries with `RichLinkCache.put(url, metaData)`, `remove(url)` and `clear()`. The cache is memory only and is gone when the app process ends.

Whether or not the cache is on, previews of the same URL that load at the same time share one network request.

### Pull-to-refresh

`RichLinks.refresh` fetches pages again, ignoring the cache. Every preview showing that URL, in Compose or in a `RichLinkView`, switches to the new data. Previews keep their current content while the refresh runs, and keep it if the refresh fails.

```kotlin
PullToRefreshBox(
    isRefreshing = refreshing,
    onRefresh = {
        scope.launch {
            refreshing = true
            val results = RichLinks.refresh(urlsOnScreen) // or RichLinks.refresh(url)
            refreshing = false
            if (results.values.any { it.isFailure }) showError()
        }
    }
) { /* previews */ }
```

From Java or View-based code (for example a `SwipeRefreshLayout`), use the callback version. The listener runs on the main thread with the URLs that failed:

```java
RichLinks.refresh(urls, failedUrls -> swipeRefreshLayout.setRefreshing(false));
```

### Java

The whole API works from Java. `RichLinkCache` members are static, and expiry takes a `TimeUnit`:

```java
RichLinkCache.setEnabled(true);
RichLinkCache.setMaxSize(200);
RichLinkCache.setExpireAfter(30, TimeUnit.MINUTES); // Long.MAX_VALUE = never expire
```

### Without UI

To fetch metadata only, use `RichPreview`: `getPreviewSuspend(url)` from a coroutine, or `getPreview(url)` with a `ResponseListener`.

## Available Styles

| Style | Description |
| :--- | :--- |
| `DEFAULT` | Standard horizontal card with image on the left. |
| `SKYPE` | Large card with background image and overlay text. |
| `TWITTER` | Twitter-inspired clean layout. |
| `TELEGRAM` | Telegram-style layout with vertical accent bar. |

## License

```
Copyright 2024 SB Dev

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
