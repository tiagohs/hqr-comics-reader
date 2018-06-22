package com.tiagohs.hqr.database.repository

import android.os.HandlerThread
import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.models.database.Source
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmResults
import java.util.concurrent.Executors




class SourceRepository: ISourceRepository {

    var looperScheduler: Scheduler? = null
    var writeScheduler: Scheduler? = null

    constructor() {
        var handlerThread = HandlerThread("LOOPER_SCHEDULER");
        handlerThread.start();

        synchronized(handlerThread) {
            looperScheduler = AndroidSchedulers.from(handlerThread.getLooper());
        }

        writeScheduler = Schedulers.from(Executors.newSingleThreadExecutor());
    }

    override fun insert(source: Source) {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction { r -> r.insertOrUpdate(source) }
    }

    override fun getSources(): RealmResults<Source> {
        val realm = Realm.getDefaultInstance()
        return realm.where(Source::class.java).findAllAsync()
    }

    override fun getSourcesAndLanguages(): RealmResults<CatalogueSource> {
        val realm = Realm.getDefaultInstance()
        return realm.where(CatalogueSource::class.java).findAllAsync()
    }

}