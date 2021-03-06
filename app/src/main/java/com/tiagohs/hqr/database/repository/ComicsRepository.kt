package com.tiagohs.hqr.database.repository

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.factory.ComicsFactory
import com.tiagohs.hqr.factory.ComicsFactory.createListOfComicModelFormRealm
import com.tiagohs.hqr.factory.ComicsFactory.createListOfComicViewModel
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic
import com.tiagohs.hqr.models.view_models.ComicViewModel
import io.reactivex.Observable
import io.realm.Case
import io.realm.Realm
import io.realm.RealmQuery

class ComicsRepository(
        private val sourceRepository: ISourceRepository
): BaseRepository(), IComicsRepository {

    override fun insertRealm(comic: ComicViewModel, sourceId: Long, skipFavorite: Boolean?): ComicViewModel? {
        val realm = Realm.getDefaultInstance()
        val localComic = ComicViewModel()

        try {
            val source: SourceDB? = sourceRepository.getSourceByIdRealm(sourceId)
            var result = realm.where(Comic::class.java)
                                      .equalTo("source.id", sourceId)
                                      .equalTo("pathLink", comic.pathLink)
                                      .findFirst()

            realm.executeTransaction { r ->
                if (result != null) {
                    result = ComicsFactory.copyFromComicViewModel(result!!, comic, source!!, r, skipFavorite)
                } else {
                    result = ComicsFactory.createComicModelForRealm(comic, source, r)
                }

                r.insertOrUpdate(result)
            }

            localComic.copyFrom(result!!)

            finishTransaction(realm)
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()

            throw ex
        }

        return localComic
    }

    override fun insertRealm(comics: List<ComicViewModel>, sourceId: Long): List<ComicViewModel>? {
        val realm = Realm.getDefaultInstance()
        var comicsLocal: List<Comic>?
        var comicsLocalFinal: List<ComicViewModel>? = null

        try {
            val source: SourceDB? = sourceRepository.getSourceByIdRealm(sourceId)
            realm.executeTransaction { r ->
                comicsLocal = createListOfComicModelFormRealm(comics, source!!, r)

                r.insertOrUpdate(comicsLocal)

                comicsLocalFinal = createListOfComicViewModel(comicsLocal!!)
            }

            finishTransaction(realm)
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()

            throw ex
        }

        return comicsLocalFinal
    }

    override fun findByPathUrlRealm(pathLink: String, sourceId: Long): ComicViewModel? {
        val realm = Realm.getDefaultInstance()

        return find(realm, realm.where(Comic::class.java)
                .equalTo("pathLink", pathLink)
                .equalTo("source.id", sourceId))
    }

    override fun findByIdRealm(comicId: Long): ComicViewModel? {
        val realm = Realm.getDefaultInstance()

        return find(realm, realm.where(Comic::class.java)
                                .equalTo("id", comicId))
    }

    override fun isFavorite(pathLink: String): Boolean {
        val realm = Realm.getDefaultInstance()
        val isFavorite = realm.where(Comic::class.java)
                .beginGroup()
                                                .equalTo("pathLink", pathLink)
                .and()
                                                .equalTo("favorite", true)
                .endGroup()
                                                .findFirst()
        finishTransaction(realm)

        return isFavorite != null
    }

    override fun insertOrUpdateComic(comic: ComicViewModel, sourceId: Long, skipFavorite: Boolean?): Observable<ComicViewModel> {
        return sourceRepository.getSourceById(sourceId)
                .map { source -> insertRealm(comic, sourceId, skipFavorite) }
    }

    override fun insertOrUpdateComics(comics: List<ComicViewModel>, sourceId: Long): Observable<List<ComicViewModel>> {
        return sourceRepository.getSourceById(sourceId)
                .map { source -> insertRealm(comics, sourceId) }
    }

    override fun searchComic(query: String, sourceId: Long): Observable<List<ComicViewModel>> {
        return Observable.create<List<ComicViewModel>> { emitter ->
            try {
                val realm = Realm.getDefaultInstance()

                emitter.onNext(findAllComics(realm, realm.where(Comic::class.java)
                        .equalTo("source.id", sourceId)
                        .contains("name", query, Case.INSENSITIVE)
                        ) )

                emitter.onComplete()
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun addOrRemoveFromFavorite(comic: ComicViewModel, sourceId: Long): Observable<ComicViewModel> {
        return sourceRepository.getSourceById(sourceId)
                .map { source ->
                    comic.favorite = !comic.favorite

                    val realm = Realm.getDefaultInstance()
                    val comicLocal = realm.where(Comic::class.java)
                            .equalTo("source.id", source.id)
                            .equalTo("pathLink", comic.pathLink)
                            .findFirst()

                    if (comicLocal != null) {
                        realm.executeTransaction { r ->
                            comicLocal.favorite = comic.favorite

                            r.insertOrUpdate(comicLocal)
                        }

                        finishTransaction(realm)

                        comic
                    } else {
                        insertRealm(comic, sourceId, false)
                    }
                }
    }

    override fun setAsDownloaded(comic: ComicViewModel, sourceId: Long): Observable<ComicViewModel>  {
        return sourceRepository.getSourceById(sourceId)
                .map { source ->
                    val realm = Realm.getDefaultInstance()

                    val comicLocal = realm.where(Comic::class.java)
                            .equalTo("source.id", source.id)
                            .equalTo("pathLink", comic.pathLink)
                            .findFirst()

                    if (comicLocal != null && !comicLocal.downloaded) {
                        realm.executeTransaction { r ->
                            comicLocal.downloaded = true
                            comic.downloaded = true

                            r.insertOrUpdate(comicLocal)
                        }

                        finishTransaction(realm)

                        comic
                    } else {
                        insertRealm(comic, sourceId, false)
                    }
                }
    }

    override fun setAsNotDownloaded(comic: ComicViewModel, sourceId: Long): Observable<ComicViewModel>  {
        return sourceRepository.getSourceById(sourceId)
                .map { source ->
                    val realm = Realm.getDefaultInstance()

                    val comicLocal = realm.where(Comic::class.java)
                            .equalTo("source.id", source.id)
                            .equalTo("pathLink", comic.pathLink)
                            .findFirst()

                    if (comicLocal != null && comicLocal.downloaded) {
                        realm.executeTransaction { r ->
                            comicLocal.downloaded = false
                            comic.downloaded = false

                            r.insertOrUpdate(comicLocal)
                        }

                        finishTransaction(realm)

                        comic
                    } else {
                        insertRealm(comic, sourceId, false)
                    }
                }
    }

    override fun deleteComic(comic: ComicViewModel): Observable<Void> {
        return delete<Comic>(comic.id)
    }

    override fun deleteAllComics(): Observable<Void?> {
        return deleteAll<Chapter>()
    }

    override fun findAll(sourceId: Long): Observable<List<ComicViewModel>> {
        return Observable.create<List<ComicViewModel>> { emitter ->
            try {
                val realm = Realm.getDefaultInstance()

                emitter.onNext(findAllComics(realm, realm.where(Comic::class.java)
                        .equalTo("source.id", sourceId) ))
                emitter.onComplete()

            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun findById(comicId: Long): Observable<ComicViewModel?> {
        return Observable.create<ComicViewModel> { emitter ->
            try {
                val realm = Realm.getDefaultInstance()
                val result = find(realm, realm.where(Comic::class.java)
                        .equalTo("id", comicId))

                if (result != null) {
                    emitter.onNext(result)
                }

                emitter.onComplete()
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun findByPathUrl(pathLink: String, sourceId: Long): Observable<ComicViewModel?> {
        return Observable.create<ComicViewModel> { emitter ->
            try {
                val realm = Realm.getDefaultInstance()

                val result = find(realm, realm.where(Comic::class.java)
                                                                .equalTo("pathLink", pathLink)
                                                                .equalTo("source.id", sourceId))

                if (result != null) {
                    emitter.onNext(result)
                }

                emitter.onComplete()
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun getPopularComics(sourceId: Long): Observable<List<ComicViewModel>> {
        return getAllByTag(sourceId, IComic.POPULARS)
    }

    override fun getRecentsComics(sourceId: Long): Observable<List<ComicViewModel>> {
        return getAllByTag(sourceId, IComic.RECENTS)
    }

    private fun getAllByTag(sourceId: Long, tag: String): Observable<List<ComicViewModel>> {
        return Observable.create<List<ComicViewModel>> { emitter ->
            try {
                val realm = Realm.getDefaultInstance()

                emitter.onNext(findAllComics(realm, realm.where(Comic::class.java)
                        .equalTo("source.id", sourceId) )
                        .filter { it.tags?.contains(tag) ?: false })

                emitter.onComplete()
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun getFavoritesComics(): Observable<List<ComicViewModel>> {
        return Observable.create<List<ComicViewModel>> { emitter ->
            try {
                val realm = Realm.getDefaultInstance()
                emitter.onNext(findAllComics(realm, realm.where(Comic::class.java)
                        .equalTo("favorite", true) ))

                emitter.onComplete()

            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun getDownloadedComics(): Observable<List<ComicViewModel>> {
        return Observable.create<List<ComicViewModel>> { emitter ->
            try {
                val realm = Realm.getDefaultInstance()
                emitter.onNext(findAllComics(realm, realm.where(Comic::class.java)
                        .equalTo("downloaded", true) ))

                emitter.onComplete()

            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun getTotalChapters(comic: ComicViewModel): Observable<Int> {
        return findById(comic.id)
                .map { c -> c.chapters?.size ?: 0 }
    }

    override fun checkIfIsSaved(comics: List<ComicViewModel>): Observable<List<ComicViewModel>> {
        return Observable.create<List<ComicViewModel>> { emitter ->
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
                    emitter.onNext(ComicsFactory.createListOfComicViewModel(results!!.toList()))
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

    private fun find(realm: Realm, realmQuery: RealmQuery<Comic>): ComicViewModel? {

        try {
            val result = realmQuery.findFirst()

            var comic: ComicViewModel? = null
            if (result != null) {
                comic = ComicsFactory.createComicViewModel(result)
            }

            finishTransaction(realm)

            return comic
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()

            throw ex
        }
    }

    private fun findAllComics(realm: Realm, realmQuery: RealmQuery<Comic>): List<ComicViewModel> {
        try {
            val results = realmQuery.findAll()

            var comics: List<ComicViewModel> = emptyList()
            if (results != null) {
                comics = ComicsFactory.createListOfComicViewModel(results.toList())
            }

            finishTransaction(realm)

            return comics
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()

            throw ex
        }
    }

}