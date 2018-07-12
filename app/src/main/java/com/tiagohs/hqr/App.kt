package com.tiagohs.hqr

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import com.facebook.stetho.Stetho
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.tiagohs.hqr.database.HQRInitialData
import com.tiagohs.hqr.dragger.components.DaggerHQRComponent
import com.tiagohs.hqr.dragger.components.HQRComponent
import com.tiagohs.hqr.dragger.modules.AppModule
import com.tiagohs.hqr.models.database.CatalogueSource
import com.uphyca.stetho_realm.RealmInspectorModulesProvider
import io.realm.Realm
import io.realm.RealmConfiguration

class App : Application() {

    private var instance: App? = null

    private var mHQRComponent: HQRComponent? = null

    override fun onCreate() {
        super.onCreate()

        onConfigureDagger()
        onConfigureRealm()
        // onConfigurePicasso()

        instance = this

        MultiDex.install(this);
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

    private fun onConfigurePicasso() {
        val build = Picasso.Builder(this)
                                        .downloader(OkHttp3Downloader(applicationContext, Integer.MAX_VALUE.toLong()))
                                        .build()

        build.setIndicatorsEnabled(true)
        build.isLoggingEnabled = true

        Picasso.setSingletonInstance(build)
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