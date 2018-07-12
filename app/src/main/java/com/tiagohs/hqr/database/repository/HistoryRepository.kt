package com.tiagohs.hqr.database.repository

import com.tiagohs.hqr.database.IHistoryRepository
import com.tiagohs.hqr.factory.HistoryFactory
import com.tiagohs.hqr.models.database.comics.ComicHistory
import com.tiagohs.hqr.models.view_models.ComicHistoryViewModel
import io.reactivex.Observable
import io.realm.Realm
import io.realm.RealmQuery

class HistoryRepository: BaseRepository(), IHistoryRepository {

    override fun insertComicHistoryRealm(comicHistoryViewModel: ComicHistoryViewModel): ComicHistoryViewModel? {
        val realm = Realm.getDefaultInstance()
        val localComicHistory = ComicHistoryViewModel()

        try {
            var result = realm.where(ComicHistory::class.java)
                                            .equalTo("comic.pathLink", comicHistoryViewModel.comic?.pathLink)
                                            .findFirst()

            realm.executeTransaction { r ->
                if (result != null) {
                    result = HistoryFactory.copyFromComicHistoryViewModel(result!!, comicHistoryViewModel, r)
                } else {
                    result = HistoryFactory.createComicHistoryForRealm(comicHistoryViewModel, r)
                }

                r.insertOrUpdate(result)
            }

            localComicHistory.copyFrom(result!!)

            finishTransaction(realm)
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()

            throw ex
        }

        return localComicHistory
    }

    override fun findByComicIdRealm(comicId: Long): ComicHistoryViewModel? {
        try {
            val realm = Realm.getDefaultInstance()
            return find(realm, realm.where(ComicHistory::class.java)
                    .equalTo("comic.id", comicId))
        } catch (ex: Exception) {
            throw ex
        }
    }

    override fun insertComicHistory(comicHistory: ComicHistoryViewModel): Observable<ComicHistory> {
        return Observable.create<ComicHistory> { emitter ->
            try {
                insertComicHistoryRealm(comicHistory)
                emitter.onComplete()
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun deleteComicHistory(comicHistory: ComicHistoryViewModel): Observable<Void> {
        return delete<ComicHistory>(comicHistory.id)
    }

    override fun delteAllComicHistory(): Observable<Void?> {
        return deleteAll<ComicHistory>()
    }

    override fun findAll(): Observable<List<ComicHistoryViewModel>> {
        return Observable.create<List<ComicHistoryViewModel>> { emitter ->
            try {
                val realm = Realm.getDefaultInstance()

                emitter.onNext(findAll(realm, realm.where(ComicHistory::class.java)))
                emitter.onComplete()

            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun findById(id: Long): Observable<ComicHistoryViewModel> {
        return Observable.create<ComicHistoryViewModel> { emitter ->
            try {
                val realm = Realm.getDefaultInstance()
                val result = find(realm, realm.where(ComicHistory::class.java)
                        .equalTo("id", id))

                if (result != null) {
                    emitter.onNext(result)
                }

                emitter.onComplete()
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun findByComicId(comicId: Long): Observable<ComicHistoryViewModel> {
        return Observable.create<ComicHistoryViewModel> { emitter ->
            try {
                val realm = Realm.getDefaultInstance()
                val result = find(realm, realm.where(ComicHistory::class.java)
                        .equalTo("comic.id", comicId))

                if (result != null) {
                    emitter.onNext(result)
                }

                emitter.onComplete()
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }


    private fun find(realm: Realm, realmQuery: RealmQuery<ComicHistory>): ComicHistoryViewModel? {

        try {
            val result = realmQuery.findFirst()

            var comicHistoryViewModel: ComicHistoryViewModel? = null
            if (result != null) {
                comicHistoryViewModel = HistoryFactory.createComicHistoryViewModel(result)
            }

            finishTransaction(realm)

            return comicHistoryViewModel
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()

            throw ex
        }
    }

    private fun findAll(realm: Realm, realmQuery: RealmQuery<ComicHistory>): List<ComicHistoryViewModel> {
        try {
            val results = realmQuery.findAll()

            var comicHistoryViewModels: List<ComicHistoryViewModel> = emptyList()
            if (results != null) {
                comicHistoryViewModels = HistoryFactory.createListOfChapterViewModel(results.toList())
            }

            finishTransaction(realm)

            return comicHistoryViewModels
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()

            throw ex
        }
    }


}