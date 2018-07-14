package com.tiagohs.hqr.database.repository

import com.tiagohs.hqr.database.IChapterRepository
import com.tiagohs.hqr.factory.ChapterFactory
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import io.reactivex.Observable
import io.realm.Realm
import io.realm.RealmQuery

class ChapterRepository: BaseRepository(), IChapterRepository {

    override fun getChapterRealm(pathLink: String, comicId: Long): ChapterViewModel? {
        val realm = Realm.getDefaultInstance()

        var chapter: ChapterViewModel? = null

        try {
            chapter = find(realm, realm.where(Chapter::class.java)
                                .equalTo("chapterPath", pathLink)
                                .equalTo("comic.id", comicId))
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()
        }

        return chapter
    }

    override fun getAllChapters(comicId: Long): Observable<List<ChapterViewModel>> {
        return Observable.create<List<ChapterViewModel>> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val results = findAll(realm, realm.where(Chapter::class.java)
                                                                    .equalTo("comic.id", comicId))

                if (results != null) {
                    emitter.onNext(results)
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

    override fun setOrUnsetAsDownloaded(chapterViewModel: ChapterViewModel, comicId: Long): ChapterViewModel {
        val realm = Realm.getDefaultInstance()

        var chapter: Chapter? = null

        try {
            chapter = realm.where(Chapter::class.java)
                    .equalTo("chapterPath", chapterViewModel.chapterPath)
                    .equalTo("comic.id", comicId)
                    .findFirst()

            if (chapter != null) {
                realm.executeTransaction { r ->
                    chapter.downloaded = !chapter.downloaded

                    r.insertOrUpdate(chapter)
                }

                chapterViewModel.downloaded = chapter.downloaded

                finishTransaction(realm)
            }
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()
        }

        return chapterViewModel
    }

    override fun removeAllDownloads(comicId: Long) {
        val realm = Realm.getDefaultInstance()

        var chapters: List<Chapter>? = null

        try {
            chapters = realm.where(Chapter::class.java)
                    .equalTo("comic.id", comicId)
                    .equalTo("downloaded", true)
                    .findAll()

            if (chapters != null && chapters.isNotEmpty()) {
                chapters.map { chapter ->
                    realm.executeTransaction { r ->
                        chapter.downloaded = !chapter.downloaded

                        r.insertOrUpdate(chapter)
                    }

                    chapter
                }
                finishTransaction(realm)
            }
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()
        }
    }

    override fun getChapter(chapterId: Long): Observable<ChapterViewModel> {
        return Observable.create<ChapterViewModel> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val result = find(realm, realm.where(Chapter::class.java)
                                            .equalTo("id", chapterId))

                if (result != null) {
                    emitter.onNext(result)
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

    override fun getChapter(pathLink: String, comicId: Long): Observable<ChapterViewModel> {
        return Observable.create<ChapterViewModel> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val result = find(realm, realm.where(Chapter::class.java)
                                        .equalTo("chapterPath", pathLink)
                                        .equalTo("comic.id", comicId))

                if (result != null) {
                    emitter.onNext(result)
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

    private fun find(realm: Realm, realmQuery: RealmQuery<Chapter>): ChapterViewModel? {

        try {
            val result = realmQuery.findFirst()

            var chapter: ChapterViewModel? = null
            if (result != null) {
                chapter = ChapterViewModel().create(result)
            }

            finishTransaction(realm)

            return chapter
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()

            throw ex
        }
    }

    private fun findAll(realm: Realm, realmQuery: RealmQuery<Chapter>): List<ChapterViewModel>? {
        try {
            val results = realmQuery.findAll()

            var chapters: List<ChapterViewModel>? = null
            if (results != null) {
                chapters = ChapterFactory.createListOfChapterViewModel(results.toList())
            }

            finishTransaction(realm)

            return chapters
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()

            throw ex
        }
    }
}