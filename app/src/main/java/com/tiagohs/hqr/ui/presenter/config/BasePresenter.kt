package com.tiagohs.hqr.ui.presenter.config

import com.tiagohs.hqr.ui.views.config.IView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BasePresenter<V : IView>(protected var mSubscribers: CompositeDisposable?) : IPresenter<V> {

    protected var mView: V? = null

    override fun onBindView(view: V) {
        mView = view
    }

    override fun onUnbindView() {

        if (mView != null)
            mView = null

        if (mSubscribers != null)
            mSubscribers!!.clear()
    }

    fun addSubscriber(disposable: Disposable) {
        if (mSubscribers != null)
            mSubscribers!!.add(disposable)
    }

}
