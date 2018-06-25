package com.tiagohs.hqr.database.repository

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic
import io.reactivex.Observable
import io.realm.Realm

class ComicsRepository: BaseRepository(), IComicsRepository {
    override fun insertComic(comic: Comic): Observable<Comic> {
        return insert(comic)
    }

    override fun insertComics(comics: List<Comic>): Observable<List<Comic>> {
        return insert(comics)
    }

    override fun deleteComic(comic: Comic): Observable<Void> {
        return delete<Comic>(comic.id)
    }

    override fun deleteAllComics(): Observable<Void?> {
        return deleteAll<Chapter>()
    }

    override fun getAllComics(): Observable<List<Comic>> {
        return startGetTransaction()
                .map { realm ->
                    val results = realm.where(Comic::class.java).findAll()
                    val comics = Comic().createList(results.toList())

                    finishTransaction(realm)

                    comics
                }
    }

    override fun getComic(comicId: Long): Observable<Comic> {
        return startGetTransaction()
                .map { realm ->
                    val result = realm.where(Comic::class.java)
                                    .equalTo("id", comicId).findFirst()
                    val comic = Comic().create(result!!)

                    finishTransaction(realm)

                    comic
                }
    }

    override fun getComic(pathLink: String, sourceId: Long): Observable<Comic> {
        return startGetTransaction()
                .map { realm ->
                    val result = realm.where(Comic::class.java)
                            .equalTo("pathLink", pathLink)
                            .equalTo("source.id", sourceId)
                            .findFirst()
                    val comic = Comic().create(result!!)

                    finishTransaction(realm)

                    comic
                }
    }

    override fun getPopularComics(): Observable<List<Comic>> {
        return startGetTransaction()
                .map { realm ->
                    val results = realm.where(Comic::class.java)
                            .equalTo("tags", IComic.POPULARS)
                            .findAll()
                    val comics = Comic().createList(results.toList())

                    finishTransaction(realm)

                    comics
                 }
    }

    override fun getRecentsComics(): Observable<List<Comic>> {
        var realmInstance: Realm? = null

        return startGetTransaction()
                .map { realm ->
                    realmInstance = realm
                    val results = realm.where(Comic::class.java)
                            .equalTo("tags", IComic.RECENTS)
                            .findAll()
                    val comics = Comic().createList(results.toList())

                    finishTransaction(realm)

                    comics
                     }
    }

    override fun getFavoritesComics(): Observable<List<Comic>> {
        return startGetTransaction()
                .map { realm ->
                    val results = realm.where(Comic::class.java)
                            .equalTo("favorite", true).findAll()
                    val comics = Comic().createList(results.toList())

                    finishTransaction(realm)

                    comics
                }
                .doOnNext { t ->  }
    }

    override fun getTotalChapters(comic: Comic): Observable<Int> {
        return getComic(comic.id)
                .map { c -> c.chapters?.size ?: 0 }
    }

}