package com.sbdev.project.richlinkpreview

interface ViewListener {

    fun onSuccess(status: Boolean)

    fun onError(e: Exception)

}