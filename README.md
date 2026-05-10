# RichLinkPreview

A powerful and lightweight Android library to generate rich link previews. Now updated with **Jetpack Compose** support, modern Coroutines, and multiple UI styles.

## Features

- 🚀 **Jetpack Compose Support**: Built-in composables for easy integration in modern apps.
- 🎨 **Multiple Styles**: Choose from `DEFAULT`, `SKYPE`, `TWITTER`, and `TELEGRAM` layouts.
- 📦 **Metadata Extraction**: Automatically fetches Title, Description, Image, Favicon, and Site Name using Jsoup.
- 🔄 **Coroutine-based**: Efficient background fetching without blocking the UI thread.
- 🖼️ **Image Loading**: Integrated with **Coil** (for Compose) and **Picasso** (for Legacy Views).
- 🛠️ **Legacy Support**: Fully compatible with traditional XML-based layouts.

## Installation

Add the following to your `build.gradle.kts` (Module: app):

```kotlin
dependencies {
    implementation(project(":richlinkpreview"))
    
    // Core dependencies used by the library
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
```

## Usage

### Jetpack Compose (Recommended)

Simply use the `RichLinkPreview` composable in your UI:

```kotlin
import com.sbdev.project.richlinkpreview.RichLinkPreview
import com.sbdev.project.richlinkpreview.RichLinkStyle

@Composable
fun MyScreen() {
    Column {
        // Default Style
        RichLinkPreview(url = "https://www.google.com")

        // Skype Style
        RichLinkPreview(
            url = "https://github.com",
            style = RichLinkStyle.SKYPE
        )
        
        // Custom Click Action
        RichLinkPreview(
            url = "https://kotlinlang.org",
            onClicked = { metaData ->
                println("Clicked on: ${metaData.title}")
            }
        )
    }
}
```

### Legacy XML Views

Add `RichLinkView` to your layout:

```xml
<com.sbdev.project.richlinkpreview.RichLinkView
    android:id="@+id/richLinkView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Initialize in your Activity/Fragment:

```kotlin
val richLinkView = findViewById<RichLinkView>(R.id.richLinkView)

richLinkView.setLink("https://www.google.com", object : ViewListener {
    override fun onSuccess(status: Boolean) { }
    override fun onError(e: Exception) { }
})
```

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
