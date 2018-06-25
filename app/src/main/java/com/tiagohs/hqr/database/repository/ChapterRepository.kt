package com.tiagohs.hqr.database.repository

import com.tiagohs.hqr.database.IChapterRepository
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic
import io.reactivex.Observable
import io.realm.Realm

class ChapterRepository: BaseRepository(), IChapterRepository {
    override fun insertChapter(chapter: Chapter): Observable<Chapter> {
        val realm = Realm.getDefaultInstance()
        val result = realm.where(Chapter::class.java)
                                  .findFirst()
        if (result != null) {
            return insert(result)
        } else {
            chapter.id = getDataId<Chapter>(realm)

            return insert(chapter)
        }
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
        return startGetTransaction()
                .map { realm ->
                    val results = realm.where(Chapter::class.java)
                                        .equalTo("comic.id", comic.id)
                                        .findAll()
                    val chapters = Chapter().createList(results.toList())

                    finishTransaction(realm)

                    chapters
                }
    }

    override fun getChapter(chapterId: Long): Observable<Chapter> {
        return startGetTransaction()
                .map { realm ->
                    val result = realm.where(Chapter::class.java)
                                    .equalTo("id", chapterId)
                                    .findFirst()
                    val chapter = Chapter().create(result!!)

                    finishTransaction(realm)

                    chapter
                }
    }

    override fun getChapter(pathLink: String, comicId: Long): Observable<Chapter> {
        return startGetTransaction()
                .map { realm ->
                    val result = realm.where(Chapter::class.java)
                            .equalTo("chapterPath", pathLink)
                            .equalTo("comic.id", comicId)
                            .findFirst()
                    val chapter = Chapter().create(result!!)

                    finishTransaction(realm)

                    chapter
                }
    }

}