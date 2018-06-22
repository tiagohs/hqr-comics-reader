package com.tiagohs.hqr.dragger.modules

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class AppModule {

    lateinit var mApplication: Application

    constructor(application: Application) {
        this.mApplication = application
    }

    @Provides
    fun providesApplication(): Application {
        return mApplication
    }

    @Provides
    fun providerApplicationContext(): Context {
        return mApplication.applicationContext
    }

}