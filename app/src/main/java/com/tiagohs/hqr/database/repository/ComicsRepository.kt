package com.tiagohs.hqr.database.repository

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic
import com.tiagohs.hqr.models.viewModels.ComicViewModel
import io.reactivex.Observable
import io.realm.Realm
import com.tiagohs.hqr.models.sources.Comic as NetworkComic
import com.tiagohs.hqr.models.sources.ComicsItem as NetworkComicsItem

class ComicsRepository(
        private val sourceRepository: ISourceRepository
): BaseRepository(), IComicsRepository {

    override fun insertRealm(comic: NetworkComicsItem, sourceId: Long, tags: List<String>?, initialized: Boolean): Comic? {
        val realm = Realm.getDefaultInstance()
        val localComic = Comic()

        try {
            val source: SourceDB? = sourceRepository.getSourceByIdRealm(sourceId)
            var result = realm.where(Comic::class.java)
                                      .equalTo("pathLink", comic.link)
                                      .findFirst()

            realm.executeTransaction { r ->
                if (result != null) {
                    result?.copyFrom(comic)
                } else {
                    result = createFromNetwork(comic, source!!, tags, initialized, realm)
                }

                r.insertOrUpdate(result)
            }

            localComic.copyFrom(result!!)

            finishTransaction(realm)
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()
        }

        return localComic
    }

    override fun insertOrUpdateComic(comic: NetworkComicsItem, sourceId: Long, tags: List<String>?, initialized: Boolean): Observable<Comic> {
        return sourceRepository.getSourceById(sourceId)
                .flatMap { source ->
                    startGetTransaction()
                            .map { realm: Realm ->
                                val localComic = Comic()
                                var result = realm.where(Comic::class.java)
                                        .equalTo("pathLink", comic.link)
                                        .findFirst()

                                realm.executeTransaction { r ->
                                    if (result != null) {
                                        result?.copyFrom(comic)
                                    } else {
                                        result = createFromNetwork(comic, source, tags, initialized, realm)
                                    }

                                    r.insertOrUpdate(result)

                                }

                                localComic.copyFrom(result!!)

                                finishTransaction(realm)
                                localComic
                            }
                }
    }

    override fun insertOrUpdateComic(comic: NetworkComic, sourceId: Long, tags: List<String>?, initialized: Boolean): Observable<ComicViewModel> {
        return sourceRepository.getSourceById(sourceId)
                               .flatMap { source ->
                                   startGetTransaction()
                                           .map { realm: Realm ->
                                               val localComic = ComicViewModel()
                                               var result = realm.where(Comic::class.java)
                                                       .equalTo("pathLink", comic.pathLink)
                                                       .findFirst()

                                               realm.executeTransaction { r ->
                                                   if (result != null) {
                                                       result?.copyFrom(comic)
                                                   } else {
                                                       result = createFromNetwork(comic, source, tags, initialized, realm)
                                                   }

                                                   r.insertOrUpdate(result)

                                               }

                                               localComic.copyFrom(result!!)

                                               finishTransaction(realm)
                                               localComic
                                           }
                               }
    }

    override fun insertOrUpdateComic(comic: Comic, initialized: Boolean): Observable<Comic> {
        comic.inicialized = initialized

        return insert(comic)
    }

    override fun insertOrUpdateComics(comics: List<Comic>): Observable<List<Comic>> {
        return insert(comics)
    }

    override fun deleteComic(comic: Comic): Observable<Void> {
        return delete<Comic>(comic.id)
    }

    override fun deleteAllComics(): Observable<Void?> {
        return deleteAll<Chapter>()
    }

    override fun getAllComics(): Observable<List<Comic>> {
        return Observable.create<List<Comic>> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val results = realm.where(Comic::class.java)
                        .findAll()

                if (results != null) {
                    val comics = Comic().createList(results.toList())
                    emitter.onNext(comics)
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

    override fun getComic(comicId: Long): Observable<Comic?> {
        return Observable.create<Comic> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val result = realm.where(Comic::class.java)
                        .equalTo("id", comicId).findFirst()

                if (result != null) {
                    val comic = Comic().create(result)
                    emitter.onNext(comic)
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

    override fun getComicRealm(pathLink: String, sourceId: Long): Comic? {
        val realm = Realm.getDefaultInstance()

        try {
            val result = realm.where(Comic::class.java)
                    .equalTo("pathLink", pathLink)
                    .equalTo("source.id", sourceId)
                    .findFirst()

            var comic: Comic? = null
            if (result != null) {
                comic = Comic().create(result)
            }

            finishTransaction(realm)

            return comic
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()
        }

        return null
    }

    override fun getComic(pathLink: String, sourceId: Long): Observable<Comic?> {
        return Observable.create<Comic> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val result = realm.where(Comic::class.java)
                        .equalTo("pathLink", pathLink)
                        .equalTo("source.id", sourceId)
                        .findFirst()

                if (result != null) {
                    val comic = Comic().create(result)
                    emitter.onNext(comic)
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

    override fun getPopularComics(): Observable<List<ComicViewModel>> {
        return Observable.create<List<ComicViewModel>> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val resultsComics = realm.where(Comic::class.java).findAll()

                if (resultsComics != null) {
                    val resultsPopulars = resultsComics.filter { it.tags!!.contains(IComic.POPULARS) }
                    val comics = ComicViewModel().createList(resultsPopulars.toList())

                    emitter.onNext(comics)
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

    override fun getRecentsComics(): Observable<List<ComicViewModel>> {
        return Observable.create<List<ComicViewModel>> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val resultsComics = realm.where(Comic::class.java).findAll()

                if (resultsComics != null) {
                    val resultsPopulars = resultsComics.filter { it.tags!!.contains(IComic.RECENTS) }
                    val comics = ComicViewModel().createList(resultsPopulars.toList())
                    emitter.onNext(comics)
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

    override fun getFavoritesComics(): Observable<List<IComic>> {
        return Observable.create<List<IComic>> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val results = realm.where(Comic::class.java)
                        .equalTo("favorite", true)
                        .findAll()

                if (results != null) {
                    val comics = Comic().createList(results.toList())
                    emitter.onNext(comics)
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

    override fun getTotalChapters(comic: Comic): Observable<Int> {
        return getComic(comic.id)
                .map { c -> c.chapters?.size ?: 0 }
    }

    override fun checkIfIsSaved(comics: List<IComic>): Observable<List<IComic>> {
        return Observable.create<List<IComic>> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val itemsId = comics.map { it.id }
                var results: ArrayList<Comic>? = null

                itemsId.forEach { id ->
                    val result = realm
                            .where(Comic::class.java)
                            .equalTo("id", id)
                            .findFirst()

                    if (result != null) {
                        if (results == null)
                            results = ArrayList<Comic>()

                        results?.add(result)
                    }
                }

                if (results != null) {
                    val comicsFinded = Comic().createList(results!!.toList())
                    emitter.onNext(comicsFinded)
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