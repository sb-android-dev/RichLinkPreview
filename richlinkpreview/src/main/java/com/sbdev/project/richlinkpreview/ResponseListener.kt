package com.sbdev.project.richlinkpreview

interface ResponseListener {

    fun onData(metaData: MetaData)

    fun onError(e: Exception)
}