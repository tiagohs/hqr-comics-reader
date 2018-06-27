package com.tiagohs.hqr.interceptors.config

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BaseInterceptor {

    protected lateinit var subscribers: CompositeDisposable

    open fun onBind() {
        subscribers = CompositeDisposable()
    }

    open fun onUnbind() {
        subscribers?.dispose()
    }

    fun addSubscriber(disposable: Disposable) {
        subscribers?.add(disposable)
    }


}