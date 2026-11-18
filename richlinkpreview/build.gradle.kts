plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "com.sbdev.project.richlinkpreview"
    compileSdk = 37

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.coroutines.android)

    // Public API types (AbstractComposeView, Modifier, Color) come from these, so expose them to consumers.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

kotlin {
    // Every public declaration must say `public` explicitly, so internals do not leak into the API.
    explicitApi()
}

// Publishing to Maven Central (Central Portal). GROUP, POM_ARTIFACT_ID and VERSION_NAME
// come from gradle.properties; credentials and the signing key come from the environment
// or ~/.gradle/gradle.properties, never from this repo. See PUBLISHING.md.
mavenPublishing {
    publishToMavenCentral()

    // Sign only when a key is configured, so `publishToMavenLocal` works on machines without one.
    // Maven Central rejects unsigned uploads, and the release workflow always provides the key.
    val hasSigningKey = providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.keyId").isPresent
    if (hasSigningKey) signAllPublications()

    pom {
        name.set("RichLinkPreview")
        description.set(
            "Rich link previews for Android, built with Jetpack Compose: built-in card styles, " +
                "custom UI, XML support, caching and pull-to-refresh."
        )
        inceptionYear.set("2024")
        url.set("https://github.com/sb-android-dev/RichLinkPreview")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("sb-android-dev")
                name.set("Smit Bhanvadia")
                url.set("https://github.com/sb-android-dev")
            }
        }
        scm {
            url.set("https://github.com/sb-android-dev/RichLinkPreview")
            connection.set("scm:git:git://github.com/sb-android-dev/RichLinkPreview.git")
            developerConnection.set("scm:git:ssh://git@github.com/sb-android-dev/RichLinkPreview.git")
        }
    }
}
