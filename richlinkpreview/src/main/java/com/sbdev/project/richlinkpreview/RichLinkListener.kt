package com.sbdev.project.richlinkpreview

import android.view.View

interface RichLinkListener {

    fun onClicked(view: View, metaData: MetaData)

}