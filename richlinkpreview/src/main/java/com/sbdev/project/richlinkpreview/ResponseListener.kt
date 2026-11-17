package com.sbdev.project.richlinkpreview

public interface ResponseListener {

    public fun onData(metaData: MetaData)

    public fun onError(e: Exception)
}