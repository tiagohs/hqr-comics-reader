package com.tiagohs.hqr.database

import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.database.comics.ComicHistory
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import io.reactivex.Observable

interface ISourceRepository {

    fun getSourceByIdRealm(sourceId: Long): SourceDB?

    fun insertSource(sourceDB: SourceDB): Observable<SourceDB>
    fun getAllSources(): Observable<List<SourceDB>?>
    fun getAllCatalogueSources(): Observable<List<CatalogueSource>?>

    fun getCatalogueSourceById(catalogueSourceId: Long): Observable<CatalogueSource?>
    fun getSourceById(sourceId: Long): Observable<SourceDB?>
}

interface IComicsRepository {

    fun insertRealm(comic: ComicViewModel, sourceId: Long): ComicViewModel?
    fun insertRealm(comics: List<ComicViewModel>, sourceId: Long): List<ComicViewModel>?
    fun findByPathUrlRealm(pathLink: String, sourceId: Long): ComicViewModel?
    fun findByIdRealm(comicId: Long): ComicViewModel?

    fun insertOrUpdateComic(comic: ComicViewModel, sourceId: Long): Observable<ComicViewModel>
    fun insertOrUpdateComics(comics: List<ComicViewModel>, sourceId: Long): Observable<List<ComicViewModel>>

    fun deleteComic(comic: ComicViewModel): Observable<Void>

    fun deleteAllComics(): Observable<Void?>
    fun findAll(sourceId: Long): Observable<List<ComicViewModel>>

    fun findById(comicId: Long): Observable<ComicViewModel?>
    fun findByPathUrl(pathLink: String, sourceId: Long):  Observable<ComicViewModel?>

    fun getPopularComics(sourceId: Long): Observable<List<ComicViewModel>>
    fun getRecentsComics(sourceId: Long): Observable<List<ComicViewModel>>
    fun getFavoritesComics(): Observable<List<ComicViewModel>>

    fun getTotalChapters(comic: ComicViewModel): Observable<Int>

    fun checkIfIsSaved(comics: List<ComicViewModel>): Observable<List<ComicViewModel>>
}

interface IChapterRepository {

    fun getChapterRealm(pathLink: String, comicId: Long): ChapterViewModel?

    fun getAllChapters(comicId: Long): Observable<List<ChapterViewModel>>

    fun getChapter(chapterId: Long): Observable<ChapterViewModel>
    fun getChapter(pathLink: String, comicId: Long): Observable<ChapterViewModel>
}

interface IHistoryRepository {

    fun insertComicHistory(comicHistory: ComicHistory): Observable<ComicHistory>
    fun deleteComicHistory(comicHistory: ComicHistory): Observable<Void>

    fun delteAllComicHistory(): Observable<Void?>
    fun getAllComicHistory(): Observable<List<ComicHistory>>

    fun getComicHistory(id: Long): Observable<ComicHistory>
}

