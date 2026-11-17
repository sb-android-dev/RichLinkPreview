package com.sbdev.project.sample

import android.app.Application
import com.sbdev.project.richlinkpreview.RichLinkCache

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // The cache is off by default. Turning it on here, once, applies to the whole app:
        // previews that come back on screen are shown from memory instead of fetched again.
        RichLinkCache.enabled = true
    }
}
