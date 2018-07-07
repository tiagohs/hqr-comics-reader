package com.tiagohs.hqr.factory

import com.tiagohs.hqr.models.database.comics.ComicHistory
import com.tiagohs.hqr.models.view_models.ComicHistoryViewModel
import io.realm.Realm

object HistoryFactory {

    fun createComicHistoryForRealm(comicHistoryViewModel: ComicHistoryViewModel, realm: Realm): ComicHistory {
        return ComicHistory().create().apply {

            if (comicHistoryViewModel.id != -1L) {
                this.id = comicHistoryViewModel.id
            }

            if (comicHistoryViewModel.lastTimeRead != null) {
                this.lastTimeRead = comicHistoryViewModel.lastTimeRead
            }

            if (comicHistoryViewModel.comic != null) {
                val comic = ComicsFactory.createComicModelForRealm(comicHistoryViewModel.comic!!, comicHistoryViewModel.comic!!.source, realm)
                this.comic = realm.copyToRealmOrUpdate(comic)

                if (comicHistoryViewModel.chapter != null) {
                    this.chapter = realm.copyToRealmOrUpdate(ChapterFactory.createChapterForRealm(comicHistoryViewModel.chapter!!, comic, realm))
                }
            }
        }
    }

    fun createComicHistoryViewModel(comicHistory: ComicHistory): ComicHistoryViewModel {
        return ComicHistoryViewModel().create(comicHistory)
    }

    fun createListOfComicHistoryForRealm(listViewModel: List<ComicHistoryViewModel>?, realm: Realm): List<ComicHistory> {
        val comicHistory = ArrayList<ComicHistory>()

        if (listViewModel != null) {
            comicHistory.addAll( listViewModel.map { createComicHistoryForRealm(it, realm) })
        }

        return comicHistory
    }

    fun createListOfChapterViewModel(comicHistoryDb: List<ComicHistory>): List<ComicHistoryViewModel> {
        val comicHistory = ArrayList<ComicHistoryViewModel>()
        comicHistory.addAll(comicHistoryDb.map { createComicHistoryViewModel(it) })

        return comicHistory
    }

    fun copyFromComicHistoryViewModel(comicHistoryViewModel: ComicHistoryViewModel, realm: Realm): ComicHistory {
        return ComicHistory().apply {

            if (!comicHistoryViewModel.lastTimeRead.isNullOrEmpty()) {
                this.lastTimeRead = comicHistoryViewModel.lastTimeRead
            }

            if (comicHistoryViewModel.comic != null) {
                this.comic = ComicsFactory.createComicModelForRealm(comicHistoryViewModel.comic!!, comicHistoryViewModel.comic!!.source, realm)

                if (comicHistoryViewModel.chapter != null) {
                    this.chapter = ChapterFactory.createChapterForRealm(comicHistoryViewModel.chapter!!, this.comic!!, realm)
                }
            }

        }
    }
}