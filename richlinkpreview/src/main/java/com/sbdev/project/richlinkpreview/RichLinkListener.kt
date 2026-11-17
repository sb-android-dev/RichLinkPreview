package com.sbdev.project.richlinkpreview

import android.view.View

public interface RichLinkListener {

    public fun onClicked(view: View, metaData: MetaData)

}