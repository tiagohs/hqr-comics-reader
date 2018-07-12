package com.tiagohs.hqr.factory

import com.tiagohs.hqr.models.database.comics.Comic
import com.tiagohs.hqr.models.database.comics.ComicHistory
import com.tiagohs.hqr.models.view_models.ComicHistoryViewModel
import io.realm.Realm

object HistoryFactory {

    fun createComicHistoryForRealm(comicHistoryViewModel: ComicHistoryViewModel, realm: Realm): ComicHistory {
        return ComicHistory().create().apply {

            if (comicHistoryViewModel.lastTimeRead != null) {
                this.lastTimeRead = comicHistoryViewModel.lastTimeRead
            }

            if (comicHistoryViewModel.comic != null) {
                this.comic = realm.copyToRealmOrUpdate(ComicsFactory.copyFromComicViewModel(comicHistoryViewModel.comic!!, comicHistoryViewModel.comic!!.source, realm, false))

                if (comicHistoryViewModel.chapter != null) {
                    this.chapter = realm.copyToRealmOrUpdate(ChapterFactory.copyFromChapterViewModel(comicHistoryViewModel.chapter!!, this.comic!!, realm))
                }
            }
        }
    }

    fun copyFromComicHistoryViewModel(comicHistory: ComicHistory, comicHistoryViewModel: ComicHistoryViewModel, realm: Realm): ComicHistory {

        if (!comicHistoryViewModel.lastTimeRead.isNullOrEmpty()) {
            comicHistory.lastTimeRead = comicHistoryViewModel.lastTimeRead
        }

        if (comicHistoryViewModel.comic != null) {
            comicHistory.comic = realm.copyToRealmOrUpdate(ComicsFactory.copyFromComicViewModel(comicHistoryViewModel.comic!!, comicHistoryViewModel.comic!!.source, realm, false))

            if (comicHistoryViewModel.chapter != null) {
                comicHistory.chapter = realm.copyToRealmOrUpdate(ChapterFactory.copyFromChapterViewModel(comicHistoryViewModel.chapter!!, comicHistory.comic!!, realm))
            }
        }

        return comicHistory
    }

    fun copyFromComicHistoryViewModel(comic: Comic, comicHistoryViewModel: ComicHistoryViewModel, realm: Realm): ComicHistory {
        return ComicHistory().apply {

            if (comicHistoryViewModel.id != -1L) {
                this.id = comicHistoryViewModel.id
            }

            if (!comicHistoryViewModel.lastTimeRead.isNullOrEmpty()) {
                this.lastTimeRead = comicHistoryViewModel.lastTimeRead
            }

            if (comicHistoryViewModel.comic != null) {
                this.comic = realm.copyToRealmOrUpdate(ComicsFactory.copyFromComicViewModel(comicHistoryViewModel.comic!!, comicHistoryViewModel.comic!!.source, realm, false))

                if (comicHistoryViewModel.chapter != null) {
                    this.chapter = realm.copyToRealmOrUpdate(ChapterFactory.copyFromChapterViewModel(comicHistoryViewModel.chapter!!, this.comic!!, realm))
                }
            }

        }
    }

    fun createComicHistoryViewModel(comicHistory: ComicHistory): ComicHistoryViewModel {
        return ComicHistoryViewModel().create(comicHistory)
    }

    fun createListOfChapterViewModel(comicHistoryDb: List<ComicHistory>): List<ComicHistoryViewModel> {
        val comicHistory = ArrayList<ComicHistoryViewModel>()
        comicHistory.addAll(comicHistoryDb.map { createComicHistoryViewModel(it) })

        return comicHistory
    }
}