package com.tiagohs.hqr.database.repository

import com.tiagohs.hqr.database.IHistoryRepository
import com.tiagohs.hqr.models.database.comics.ComicHistory
import io.reactivex.Observable

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
        return startGetTransaction()
                .map { realm ->
                    val results = realm.where(ComicHistory::class.java).findAll()
                    val comicsHistories= ComicHistory().createList(results.toList())

                    finishTransaction(realm)

                    comicsHistories
                }
    }

    override fun getComicHistory(id: Long): Observable<ComicHistory> {
        return startGetTransaction()
                .map { realm ->
                    val result = realm.where(ComicHistory::class.java)
                            .equalTo("id", id)
                            .findFirst()
                    val comicHistory = ComicHistory().create(result!!)

                    finishTransaction(realm)

                    comicHistory
                }
    }

}