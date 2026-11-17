# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android library that fetches a URL's metadata (title, description, image, favicon, site name) with Jsoup and renders it as a link preview card. Two modules:

- `:richlinkpreview` — the library (namespace `com.sbdev.project.richlinkpreview`). It is what ships.
- `:sample` — a Compose sample app (`com.sbdev.project.sample.MainActivity`, applicationId `com.sbdev.project.richlinkpreview.sample`) that demonstrates every entry point. Use it for manual checks.

Toolchain: Gradle 9.9 (wrapper), AGP 9.5, Kotlin 2.4, Gradle daemon JVM 21, Java 11 bytecode. Kotlin support is built into AGP 9, so the module build files have no `kotlin-android` plugin, only `kotlin.compose`.

SDK levels: both modules use `compileSdk = 37` (a plain integer, not the `compileSdk { release(...) }` block) and `minSdk = 24`. The sample sets `targetSdk = 37`.

All dependency versions live in `gradle/libs.versions.toml`. Main library deps: Jsoup 1.24, Coil 2.7 (Compose), Picasso 2.71828 (Views), kotlinx-coroutines 1.11, Compose BOM 2026.09.00. The dependency snippet in `README.md` lists its own versions; keep it in sync with the catalog when upgrading.

## Commands

Use `./gradlew` from Git Bash, or `gradlew.bat` from PowerShell/cmd.

```bash
./gradlew :richlinkpreview:assembleRelease      # build the library AAR
./gradlew :sample:installDebug                  # install the sample on a device or emulator
./gradlew :richlinkpreview:testDebugUnitTest    # JVM unit tests for the library
./gradlew :richlinkpreview:testDebugUnitTest --tests "com.sbdev.project.richlinkpreview.ExampleUnitTest"   # one test class
./gradlew :richlinkpreview:connectedDebugAndroidTest   # instrumented tests (needs a device)
./gradlew lint
```

`RichPreviewParseTest` covers the metadata extraction against inline HTML with no network access, `RichLinkCacheTest` covers the cache (with a fake clock for expiry), and `RichLinkLoaderTest` covers fetch sharing and refresh with a fake fetcher, and `JavaApiTest.java` (Java on purpose) breaks the build if the public API stops being usable from Java. Run one class with `--tests "com.sbdev.project.richlinkpreview.RichPreviewParseTest"`.

## Architecture

There is a single UI implementation, in Compose. XML users get the same cards through a View wrapper.

**Fetching: `RichPreview.kt`**
- `fetch(url): Result<MetaData>` is internal. It runs Jsoup on `Dispatchers.IO`, catches every exception except cancellation, and is the path the Compose state uses.
- `getPreviewSuspend(url): MetaData?` and the callback-based `getPreview(url)` with a `ResponseListener` are the public API for callers without UI.
- `RichPreview.parse(doc, url)` is pure, so unit tests can call it directly. Keep it free of Android APIs: `resolveURL` uses `java.net.URI`, not `URLUtil`, for that reason.
- Extraction fallbacks: title is `og:title`, then `<title>`. Description is `meta[name=description]`, then `Description`, then `og:description`. Image is `og:image` (property, then name), then `image_src`, then `apple-touch-icon`, then `icon`. Favicon is `apple-touch-icon`, then `icon`. `url` is `og:url`, then the host.
- `MetaData` is an immutable `@Immutable` data class, and a missing field is `""`.

**Compose API**
- `RichLinkState.kt`: `rememberRichLinkState(url)` uses `produceState` to expose `Loading`, `Success(metaData)` or `Error(exception)`. Everything else is built on it, including `RichLinkView`, so caching applies everywhere.
- `RichLinkCache.kt`: a process-wide LRU cache (a plain synchronized `LinkedHashMap`, so it is JVM-testable). It is **off by default**; apps opt in once with `RichLinkCache.enabled = true`, and there is deliberately no per-preview flag. While disabled, `get` returns null and `put` is a no-op. Entries expire after `expireAfter` (default 1 hour), checked on read using an internal monotonic `clock` that tests replace. It caches successes only, and the key includes the user agent. The sample turns the cache on in `SampleApplication`.
- `RichLinkLoader.kt` (internal): the only path previews use to get data. It reads the cache, then keeps at most one fetch per cache key, shared by every caller. Fetches run in the loader's own scope so a cancelled caller does not cancel them for others, and the result is cached *before* the fetch leaves the in-flight map, so there is no gap where a new caller refetches. `RichPreview` itself never caches or shares; it is the raw fetcher. Every successful fetch is also emitted on an `updates(url)` flow (no replay); `rememberRichLinkState` collects it, which is how `RichLinks.refresh` reaches previews already on screen without a Loading flash.
- `RichLinks.kt`: public entry point for app-wide actions. `refresh(url)` / `refresh(urls)` are suspend; `refresh(urls, RefreshListener)` is the callback version for Java/Views, delivered on the main thread.
- `RichLinkPreview.kt` has two `RichLinkPreview` overloads plus `RichLinkCard(metaData, style)`:
  - one for the built-in `style`;
  - one for a custom card, taking a trailing `content: (MetaData) -> Unit`.
- In the built-in-style overload, `colors` must stay the **last** parameter. If a lambda parameter were last, `RichLinkPreview(url) { ... }` would be ambiguous with the `content` overload.
- `RichLinkCards.kt` holds the four public cards (`DefaultRichLinkCard`, `SkypeRichLinkCard` and so on), their shared private text and image helpers, `Modifier.richLinkClick` (calls `onClicked`, or opens the browser) and the `@Preview`s.
- `RichLinkColors.kt`: cards take their colours from `RichLinkColors`, whose defaults come from `MaterialTheme.colorScheme` via `RichLinkDefaults.colors()`. Don't hard-code colours in the cards. The Skype card's white text over its image is the intended exception.

**XML bridge: `RichLinkView.kt`**
- `RichLinkView` is an `AbstractComposeView` that renders `RichLinkCard`. It keeps the old View API (`setLink(url, ViewListener)`, `setLinkFromMeta`, `setClickListener(RichLinkListener)`, `setDefaultClickListener`) for compatibility.
- The `app:richLinkStyle` attribute is declared in `res/values/attrs.xml`. Its enum values must stay in the same order as `RichLinkStyle`.
- Enum value names become `R.id` fields, so Java keywords such as `default` are not allowed; the default style is named `defaultStyle`.
- Don't name a private property `style` in this class: its generated setter clashes with the public `setStyle()`.

**Explicit API mode** is on for the library (`kotlin { explicitApi() }`): every public declaration needs an explicit `public` modifier, and anything not meant for consumers should be `internal` or `private`.

**Java callers are supported.** Members of public `object`s get `@JvmStatic`, Kotlin-only types in signatures (such as `Duration`, `Result`, suspend functions) need a Java-friendly companion (`setExpireAfter(Long, TimeUnit)`, the `RefreshListener` callback), and `JavaApiTest.java` must keep compiling.

**Dependencies:** Compose `ui` and `ui-graphics` are declared as `api`, because public signatures expose `AbstractComposeView`, `Modifier` and `Color`. Picasso, appcompat, material and ViewBinding were removed together with the old XML layouts.

**Adding a style** takes three steps:
- add a `RichLinkStyle` entry;
- add a public card in `RichLinkCards.kt` and its branch in `RichLinkCard`;
- add the matching enum value in `attrs.xml`.

The library declares the `INTERNET` permission in its own manifest, so it merges into consuming apps.

The sample app (`:sample`) `MainActivity` exercises every entry point: the built-in styles, the custom slot, `rememberRichLinkState` including the error case, and `RichLinkView` hosted through `AndroidView`.
