package com.tiagohs.hqr.database

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic
import com.tiagohs.hqr.models.database.comics.ComicHistory
import com.tiagohs.hqr.models.sources.SimpleItem
import com.tiagohs.hqr.models.viewModels.ComicViewModel
import io.reactivex.Observable
import io.realm.Realm
import io.realm.RealmList
import com.tiagohs.hqr.models.sources.Chapter as NetworkChapter
import com.tiagohs.hqr.models.sources.Comic as NetworkComic
import com.tiagohs.hqr.models.sources.ComicsItem as NetworkComicsItem

interface ISourceRepository {

    fun getSourceByIdRealm(sourceId: Long): SourceDB?

    fun insertSource(sourceDB: SourceDB): Observable<SourceDB>
    fun getAllSources(): Observable<List<SourceDB>?>
    fun getAllCatalogueSources(): Observable<List<CatalogueSource>?>

    fun getCatalogueSourceById(catalogueSourceId: Long): Observable<CatalogueSource?>
    fun getSourceById(sourceId: Long): Observable<SourceDB?>
}

interface IComicsRepository {

    fun insertRealm(comic: NetworkComicsItem, sourceId: Long, tags: List<String>? = null, initialized: Boolean = true): Comic?
    fun getComicRealm(pathLink: String, sourceId: Long): Comic?

    fun insertOrUpdateComic(comic: NetworkComicsItem, sourceId: Long, tags: List<String>? = null, initialized: Boolean = true): Observable<Comic>
    fun insertOrUpdateComic(comic: NetworkComic, sourceId: Long, tags: List<String>? = null, initialized: Boolean = true): Observable<ComicViewModel>
    fun insertOrUpdateComic(comic: Comic, initialized: Boolean = true): Observable<Comic>
    fun insertOrUpdateComics(comics: List<Comic>): Observable<List<Comic>>

    fun deleteComic(comic: Comic): Observable<Void>

    fun deleteAllComics(): Observable<Void?>
    fun getAllComics(): Observable<List<Comic>>

    fun getComic(comicId: Long): Observable<Comic?>
    fun getComic(pathLink: String, sourceId: Long):  Observable<Comic?>

    fun getPopularComics(): Observable<List<ComicViewModel>>
    fun getRecentsComics(): Observable<List<ComicViewModel>>
    fun getFavoritesComics(): Observable<List<IComic>>

    fun getTotalChapters(comic: Comic): Observable<Int>

    fun checkIfIsSaved(comics: List<IComic>): Observable<List<IComic>>

    fun createFromNetwork(comicItemNetwork: NetworkComicsItem, source: SourceDB, tags: List<String>?, initialized: Boolean, realm: Realm): Comic {
        return Comic().create().apply {
            this.name = comicItemNetwork.title
            this.pathLink = comicItemNetwork.link
            this.inicialized = initialized

            if (tags != null) {
                if (this.tags == null) this.tags = RealmList()

                this.tags?.addAll(createListOfTagsFromNetwork(tags, realm))
            }
        }
    }

    fun createFromNetwork(comicNetwork: NetworkComic, source: SourceDB, tags: List<String>?, initialized: Boolean, realm: Realm): Comic {
        return Comic().create().apply {
            copyFrom(comicNetwork, source)

            if (comicNetwork.publisher != null) {
                this.publisher = createListOfDefaultModelFromNetwork(comicNetwork.publisher, realm)
            }

            if (comicNetwork.genres != null) {
                this.genres = createListOfDefaultModelFromNetwork(comicNetwork.genres, realm)
            }

            if (comicNetwork.authors != null) {
                this.authors = createListOfDefaultModelFromNetwork(comicNetwork.authors, realm)
            }

            if (comicNetwork.authors != null) {
                this.chapters = createListOfChaptersFromNetwork(comicNetwork.chapters, this, realm)
            }

            if (tags != null) {
                if (this.tags == null) this.tags = RealmList()

                this.tags?.addAll(createListOfTagsFromNetwork(tags, realm))
            }

            this.inicialized = inicialized
        }
    }

    fun createListOfDefaultModelFromNetwork(listNetwork: List<SimpleItem>?, realm: Realm): RealmList<DefaultModel> {
        val list = RealmList<DefaultModel>()

        listNetwork?.forEach {
            val realmObject = realm.createObject(DefaultModel::class.java, RealmUtils.getDataId<DefaultModel>(realm))

            realmObject.apply {
                this.name = it.title
                this.pathLink = it.link
            }

            list.add(realmObject)
        }

        return list
    }

    fun createListOfChaptersFromNetwork(listNetwork: List<NetworkChapter>?, comic: Comic, realm: Realm): RealmList<Chapter> {
        val list = RealmList<Chapter>()

        listNetwork?.forEach {
            val realmObject = realm.createObject(Chapter::class.java, RealmUtils.getDataId<Chapter>(realm))

            realmObject.apply {
                this.chapterPath = it.chapterPath
            }

            list.add(realmObject)
        }

        return list
    }

    fun createListOfTagsFromNetwork(tags: List<String>, realm: Realm): RealmList<String> {
        val list = RealmList<String>()

        tags.forEach {
            list.add(it)
        }

        return list
    }
}

interface IChapterRepository {

    fun insertChapter(chapter: Chapter): Observable<Chapter>
    fun insertChapters(chapters: List<Chapter>): Observable<List<Chapter>>
    fun deleteChapter(chapter: Chapter): Observable<Void>
    fun deleteChapters(chapters: List<Chapter>): Observable<Void>

    fun deleteAllChapters(): Observable<Void?>
    fun getAllChapters(comic: Comic): Observable<List<Chapter>>

    fun getChapter(chapterId: Long): Observable<Chapter>
    fun getChapter(pathLink: String, comicId: Long): Observable<Chapter>
}

interface IHistoryRepository {

    fun insertComicHistory(comicHistory: ComicHistory): Observable<ComicHistory>
    fun deleteComicHistory(comicHistory: ComicHistory): Observable<Void>

    fun delteAllComicHistory(): Observable<Void?>
    fun getAllComicHistory(): Observable<List<ComicHistory>>

    fun getComicHistory(id: Long): Observable<ComicHistory>
}

