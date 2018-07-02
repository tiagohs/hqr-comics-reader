package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.adapters.chapters.ChapterItem
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class ComicChaptersContract {

    interface IComicChaptersView: IView {

        fun onNextChapters(chapters: List<ChapterItem>)
        fun onChapterDeleted()
        fun onChapterDeletedError()
        fun onChapterStatusChange(chapterItem: ChapterItem, status: String)
    }

    interface IComicChaptersPresenter: IPresenter<IComicChaptersView> {

        fun onCreate(comic: ComicViewModel)
        fun downloadChapters(chapters: List<ChapterItem>)
        fun deleteChapters(chapters: List<ChapterItem>)

    }
}