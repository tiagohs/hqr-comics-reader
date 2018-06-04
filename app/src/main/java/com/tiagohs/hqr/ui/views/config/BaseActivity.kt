package com.tiagohs.hqr.ui.views.config

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.tiagohs.hqr.App
import com.tiagohs.hqr.dragger.components.HQRComponent
import com.tiagohs.hqr.ui.views.IActivityCallbacks
import com.tiagohs.hqr.utils.ServerUtils
import kotlinx.android.synthetic.main.activity_root.*

abstract class BaseActivity : AppCompatActivity(), IActivityCallbacks {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(onGetLayoutViewId());

        onSetupToolbar()
    }

    private fun onSetupToolbar() {

        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowTitleEnabled(false)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (onGetMenuLayoutId() != 0)
            menuInflater.inflate(onGetMenuLayoutId(), menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> return false
        }
    }

    protected fun startFragment(fragmentID: Int, fragment: Fragment) {
        val fm = supportFragmentManager
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
        return (application as App).getHQRComponent()
    }


    fun isInternetConnected(): Boolean {
        return ServerUtils.isNetworkConnected(this)
    }

    fun isAdded(): Boolean {
        return !isDestroyed
    }

    override fun setScreenTitle(title: String?) {
        if (null != title && null != toolbar)
            toolbar!!.setTitle(title)
    }

    override fun setScreenSubtitle(title: String?) {
        if (null != title && null != toolbar)
            toolbar!!.setSubtitle(title)
    }

    abstract fun onGetLayoutViewId() : Int

    abstract fun onGetMenuLayoutId(): Int
}