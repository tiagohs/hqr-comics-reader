package com.tiagohs.hqr.database.repository

import io.reactivex.Observable
import io.realm.Realm
import io.realm.RealmObject



abstract class BaseRepository {


    protected fun startGetTransaction(): Observable<Realm> {
        return Observable.create<Realm> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                emitter.onNext(realm)
                emitter.onComplete()
            } catch (ex: Exception) {
                if (!realm.isClosed)
                    realm.close()

                emitter.onError(ex)
            }
        }
    }

    protected fun finishTransaction(realmInstance: Realm?) {
        if (realmInstance != null && !realmInstance.isClosed) realmInstance.close()
    }


    protected inline fun <reified T: RealmObject> insert(data: T): Observable<T> {
        return startGetTransaction()
                    .map { realm: Realm ->
                        val result = realm.where(T::class.java).findFirst()

                        realm.executeTransaction { r ->

                            if (result == null) {
                                r.insertOrUpdate(data)
                            }
                        }

                        realm.commitTransaction()
                        finishTransaction(realm)
                        data
                    }
    }

    protected fun <T: RealmObject> insert(data: List<T>): Observable<List<T>> {
        return startGetTransaction()
                .map { realm: Realm ->
                    realm.executeTransaction { r -> r.insertOrUpdate(data) }

                    realm.commitTransaction()
                    finishTransaction(realm)
                    data
                }
    }

    protected inline fun <reified T: RealmObject> deleteAll(): Observable<Void?> {
        return startGetTransaction()
                .map { realm: Realm ->
                    val results = realm
                            .where(T::class.java)
                            .findAll()

                    realm.executeTransaction { r ->
                        results?.deleteAllFromRealm()
                    }

                    realm.commitTransaction()
                    finishTransaction(realm)

                    null
                }
    }

    protected inline fun <reified T: RealmObject> delete(id: Long): Observable<Void> {
        return startGetTransaction()
                .map { realm: Realm ->
                    val results = realm
                            .where(T::class.java)
                            .equalTo("id", id)
                            .findFirst()

                    realm.executeTransaction { r ->
                        results?.deleteFromRealm()
                    }

                    realm.commitTransaction()
                    finishTransaction(realm)

                    null
                }
    }

    protected inline fun <reified T: RealmObject> delete(itemsId: List<Long>): Observable<Void> {
        return startGetTransaction()
                .map { realm: Realm ->
                    realm.executeTransaction { r -> findAndDelete<T>(r, itemsId)}

                    realm.commitTransaction()
                    finishTransaction(realm)

                    null
                }
    }

    protected inline fun <reified T: RealmObject> findAndDelete(realm: Realm, itemsId: List<Long>) {

        itemsId.forEach { id ->
            val results = realm
                    .where(T::class.java)
                    .equalTo("id", id)
                    .findFirst()

            results?.deleteFromRealm()
        }
    }

}