package com.tiagohs.hqr.ui.views.config

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tiagohs.hqr.App
import com.tiagohs.hqr.dragger.components.HQRComponent

abstract class BaseFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(getViewID(), container, false)
        return view
    }

    protected fun getApplicationComponent(): HQRComponent? {
        return (activity!!.application as App).getHQRComponent()
    }

    abstract fun getViewID(): Int
}