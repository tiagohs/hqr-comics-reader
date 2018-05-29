package com.tiagohs.hqr.ui.views.config

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import com.tiagohs.hqr.App
import com.tiagohs.hqr.dragger.components.HQRComponent
import kotlinx.android.synthetic.main.activity_root.*

abstract class BaseActivity : AppCompatActivity() {

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

    protected fun getApplicationComponent(): HQRComponent? {
        return (application as App).getHQRComponent()
    }


    fun isInternetConnected(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    abstract fun onGetLayoutViewId() : Int

    abstract fun onGetMenuLayoutId(): Int
}