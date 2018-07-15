package com.tiagohs.hqr.ui.views.config

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tiagohs.hqr.App
import com.tiagohs.hqr.R
import com.tiagohs.hqr.dragger.components.HQRComponent
import com.tiagohs.hqr.helpers.extensions.snack
import com.tiagohs.hqr.helpers.utils.ServerUtils
import com.tiagohs.hqr.ui.views.IActivityCallbacks

abstract class BaseFragment: Fragment() {

    var activityCallbacks: IActivityCallbacks? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        activityCallbacks = context as IActivityCallbacks
    }

    override fun onDetach() {
        super.onDetach()

        activityCallbacks = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(getViewID(), container, false)
        return view
    }

    protected fun startFragment(fragmentID: Int, fragment: Fragment) {
        val fm = childFragmentManager
        val f = fm.findFragmentById(fragmentID)

        if (null == f) {
            fm.beginTransaction()
                    .add(fragmentID, fragment)
                    .commitAllowingStateLoss()
        } else {
            fm.beginTransaction()
                    .replace(fragmentID, fragment)
                    .commitAllowingStateLoss()
        }
    }

    protected fun getApplicationComponent(): HQRComponent? {
        return (activity!!.application as App).getHQRComponent()
    }


    fun isInternetConnected(): Boolean {
        return ServerUtils.isNetworkConnected(context)
    }

    abstract fun getViewID(): Int

    fun dismissSnack() {
        (activity as BaseActivity).snack?.dismiss()
    }

    open fun onError(ex: Throwable, message: Int) {

        val finalMessage = if (message != 0) {
            R.string.unknown_error
        } else {
            message
        }

        (activity as BaseActivity).snack?.dismiss()
        (activity as BaseActivity).snack = activity?.snack(getString(finalMessage), Snackbar.LENGTH_INDEFINITE) {
            setAction(R.string.action_retry) {
                onErrorAction()
            }
        }
    }

    abstract fun onErrorAction()
}