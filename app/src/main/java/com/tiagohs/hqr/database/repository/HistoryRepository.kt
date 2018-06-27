package com.tiagohs.hqr.database.repository

import com.tiagohs.hqr.database.IHistoryRepository
import com.tiagohs.hqr.models.database.comics.ComicHistory
import io.reactivex.Observable
import io.realm.Realm

class HistoryRepository: BaseRepository(), IHistoryRepository {

    override fun insertComicHistory(comicHistory: ComicHistory): Observable<ComicHistory> {
        return insert(comicHistory)
    }

    override fun deleteComicHistory(comicHistory: ComicHistory): Observable<Void> {
        return delete<ComicHistory>(comicHistory.id)
    }

    override fun delteAllComicHistory(): Observable<Void?> {
        return deleteAll<ComicHistory>()
    }

    override fun getAllComicHistory(): Observable<List<ComicHistory>> {
        return Observable.create<List<ComicHistory>> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val results = realm.where(ComicHistory::class.java)
                            .findAll()
                if (results != null) {
                    val comicsHistories= ComicHistory().createList(results.toList())
                    emitter.onNext(comicsHistories)
                }

                finishTransaction(realm)

                emitter.onComplete()
            } catch (ex: Exception) {
                if (!realm.isClosed)
                    realm.close()

                emitter.onError(ex)
            }
        }
    }

    override fun getComicHistory(id: Long): Observable<ComicHistory> {
        return Observable.create<ComicHistory> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val result = realm.where(ComicHistory::class.java)
                        .equalTo("id", id)
                        .findFirst()
                if (result != null) {
                    val comicHistory = ComicHistory().create(result)
                    emitter.onNext(comicHistory)
                }

                finishTransaction(realm)

                emitter.onComplete()
            } catch (ex: Exception) {
                if (!realm.isClosed)
                    realm.close()

                emitter.onError(ex)
            }
        }
    }

}