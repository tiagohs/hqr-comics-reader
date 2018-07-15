package com.tiagohs.hqr.ui.views.config

interface IView {

    fun isInternetConnected(): Boolean
    fun isAdded(): Boolean

    fun onError(ex: Throwable, message: Int = 0)
}