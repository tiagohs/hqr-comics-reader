package com.tiagohs.hqr

import android.app.Application
import android.content.Context
import com.tiagohs.hqr.dragger.components.DaggerHQRComponent
import com.tiagohs.hqr.dragger.components.HQRComponent
import com.tiagohs.hqr.dragger.modules.AppModule

class App : Application() {

    val TAG = App::class.java.simpleName
    private var instance: App? = null

    private var mHQRComponent: HQRComponent? = null

    override fun onCreate() {
        super.onCreate()

        mHQRComponent = DaggerHQRComponent.builder()
                .appModule(AppModule(this))
                .build()

        instance = this
    }

    fun getHQRComponent(): HQRComponent? {
        return mHQRComponent
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    fun getInstance(): App? {
        return instance
    }
}