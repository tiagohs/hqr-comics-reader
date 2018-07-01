package com.tiagohs.hqr.ui.presenter.config

import com.tiagohs.hqr.ui.views.config.IView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BasePresenter<V : IView>() : IPresenter<V> {

    protected var mView: V? = null
    protected var mSubscribers: CompositeDisposable = CompositeDisposable()

    override fun onBindView(view: V) {
        mView = view
    }

    override fun onUnbindView() {

        if (mView != null)
            mView = null

        mSubscribers.clear()
    }

    fun addSubscriber(disposable: Disposable) {
        mSubscribers.add(disposable)
    }

}
