package com.tiagohs.hqr.ui.views.config

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.tiagohs.hqr.App
import com.tiagohs.hqr.BuildConfig
import com.tiagohs.hqr.R
import com.tiagohs.hqr.dragger.components.HQRComponent
import com.tiagohs.hqr.helpers.extensions.getResourceColor
import com.tiagohs.hqr.helpers.extensions.snack
import com.tiagohs.hqr.helpers.extensions.toast
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

    fun openUrl(url: String?) {
        val context = view?.context ?: return

        if (!url.isNullOrEmpty()) {
            try {
                val urlUri = Uri.parse(url)
                val intent = CustomTabsIntent.Builder()
                        .setToolbarColor(context.getResourceColor(R.color.colorPrimary))
                        .setShowTitle(true)
                        .build()
                intent.launchUrl(activity, urlUri)
            } catch (e: Exception) {
                context.toast(e.message)
            }
        }
    }

    fun isInternetConnected(): Boolean {
        return ServerUtils.isNetworkConnected(context)
    }

    abstract fun getViewID(): Int

    fun dismissSnack() {
        (activity as BaseActivity).snack?.dismiss()
    }


    open fun onError(ex: Throwable, message: Int, withAction: Boolean = false) {

        val finalMessage = if (message == 0) {
            R.string.unknown_error
        } else {
            message
        }

        if (withAction) {

            (activity as BaseActivity).snack?.dismiss()
            (activity as BaseActivity).snack = activity?.snack(getString(finalMessage), Snackbar.LENGTH_INDEFINITE) {
                setAction(R.string.action_retry) {
                    onErrorAction()
                }
            }
        } else {
            activity?.toast(finalMessage)
        }
    }

    fun showAd(adView: AdView) {
        adView.loadAd(AdRequest.Builder().build())
    }

    abstract fun onErrorAction()
}