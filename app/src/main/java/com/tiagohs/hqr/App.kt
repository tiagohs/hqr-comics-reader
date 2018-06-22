package com.tiagohs.hqr

import android.app.Application
import android.content.Context
import com.tiagohs.hqr.database.HQRInitialData
import com.tiagohs.hqr.dragger.components.DaggerHQRComponent
import com.tiagohs.hqr.dragger.components.HQRComponent
import com.tiagohs.hqr.dragger.modules.AppModule
import com.tiagohs.hqr.models.database.CatalogueSource
import io.realm.Realm
import io.realm.RealmConfiguration

class App : Application() {

    private var instance: App? = null

    private var mHQRComponent: HQRComponent? = null

    override fun onCreate() {
        super.onCreate()

        onConfigureDagger()
        onConfigureRealm()

        instance = this
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
                                this.sources= catalogueSource.sources
                            }
                        }

                        realm.close()
                    })
                build()
        })
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