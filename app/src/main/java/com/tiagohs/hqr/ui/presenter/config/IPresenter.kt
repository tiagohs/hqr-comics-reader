package com.tiagohs.hqr.ui.presenter.config

import com.tiagohs.hqr.ui.views.config.IView

interface IPresenter<V : IView> {

    fun onBindView(view: V)
    fun onUnbindView()
}