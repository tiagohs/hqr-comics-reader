package com.tiagohs.hqr

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import com.crashlytics.android.Crashlytics
import com.evernote.android.job.JobManager
import com.facebook.stetho.Stetho
import com.google.android.gms.ads.MobileAds
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.tiagohs.hqr.database.HQRInitialData
import com.tiagohs.hqr.dragger.components.DaggerHQRComponent
import com.tiagohs.hqr.dragger.components.HQRComponent
import com.tiagohs.hqr.dragger.modules.AppModule
import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.notification.Notifications
import com.tiagohs.hqr.updater.UpdaterJob
import com.uphyca.stetho_realm.RealmInspectorModulesProvider
import io.fabric.sdk.android.Fabric
import io.realm.Realm
import io.realm.RealmConfiguration
import io.reactivex.plugins.RxJavaPlugins



class App : Application() {

    private var instance: App? = null

    private var mHQRComponent: HQRComponent? = null

    override fun onCreate() {
        super.onCreate()

        onConfigureDagger()
        onConfigureRealm()
        setupNotificationChannels()
        setupJobManager()
        onConfigurePicasso()

        instance = this

        MultiDex.install(this);
        Fabric.with(this, Crashlytics())
        MobileAds.initialize(this, "ADMOB_APP_ID")

        RxJavaPlugins.setErrorHandler { throwable -> }
    }

    private fun onConfigureDagger() {
        mHQRComponent = DaggerHQRComponent.builder()
                .appModule(AppModule(this))
                .build()
    }

    private fun onConfigureRealm() {
        Realm.init(this)

        Realm.setDefaultConfiguration(
                with(RealmConfiguration.Builder()) {
                    name("hqr_db.realm")
                    schemaVersion(1)

                    deleteRealmIfMigrationNeeded()
                    initialData( { realm ->
                        HQRInitialData.initialData(realm).forEach { catalogueSource: CatalogueSource ->
                            realm.createObject(CatalogueSource::class.java, catalogueSource.id).apply {
                                this.language = catalogueSource.language
                                this.sourceDBS = catalogueSource.sourceDBS
                            }
                        }

                        realm.close()
                    })
                build()
        })

        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(
                                RealmInspectorModulesProvider
                                        .builder(this)
                                        .build())
                        .build());
    }

    protected fun setupJobManager() {
        JobManager.create(this).addJobCreator { tag ->
            when (tag) {
                UpdaterJob.TAG -> UpdaterJob()
                else -> null
            }
        }
    }


    private fun onConfigurePicasso() {
        val build = Picasso.Builder(this)
                                        //.downloader(OkHttp3Downloader(applicationContext, Integer.MAX_VALUE.toLong()))
                                        .build()

        build.setIndicatorsEnabled(true)
        build.isLoggingEnabled = true

        Picasso.setSingletonInstance(build)
    }

    fun getHQRComponent(): HQRComponent? {
        return mHQRComponent
    }

    private fun setupNotificationChannels() {
        Notifications.createChannels(this)
    }

    fun getInstance(): App? {
        return instance
    }
}