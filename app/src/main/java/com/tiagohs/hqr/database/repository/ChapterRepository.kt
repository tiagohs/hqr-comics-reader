package com.tiagohs.hqr.database.repository

import com.tiagohs.hqr.database.IChapterRepository
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic
import io.reactivex.Observable
import io.realm.Realm

class ChapterRepository: BaseRepository(), IChapterRepository {
    override fun insertChapter(chapter: Chapter): Observable<Chapter> {
        return insert(chapter)
    }

    override fun insertChapters(chapters: List<Chapter>): Observable<List<Chapter>> {
        return insert(chapters)
    }

    override fun deleteChapter(chapter: Chapter): Observable<Void> {
        return delete<Chapter>(chapter.id)
    }

    override fun deleteChapters(chapters: List<Chapter>): Observable<Void> {
        return delete<Chapter>(chapters.map { chapter -> chapter.id })
    }

    override fun deleteAllChapters(): Observable<Void?> {
        return deleteAll<Chapter>()
    }

    override fun getAllChapters(comic: Comic): Observable<List<Chapter>> {
        return Observable.create<List<Chapter>> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val results = realm.where(Chapter::class.java)
                        .equalTo("comic.id", comic.id)
                        .findAll()

                if (results != null) {
                    val chapters = Chapter().createList(results.toList())
                    emitter.onNext(chapters)
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

    override fun getChapter(chapterId: Long): Observable<Chapter> {
        return Observable.create<Chapter> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val result = realm.where(Chapter::class.java)
                        .equalTo("id", chapterId)
                        .findFirst()

                if (result != null) {
                    val chapter = Chapter().create(result)
                    emitter.onNext(chapter)
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

    override fun getChapter(pathLink: String, comicId: Long): Observable<Chapter> {
        return Observable.create<Chapter> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val result = realm.where(Chapter::class.java)
                        .equalTo("chapterPath", pathLink)
                        .equalTo("comic.id", comicId)
                        .findFirst()

                if (result != null) {
                    val chapter = Chapter().create(result)
                    emitter.onNext(chapter)
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