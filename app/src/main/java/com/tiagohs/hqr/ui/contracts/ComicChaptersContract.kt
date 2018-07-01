package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class ComicChaptersContract {

    interface IComicChaptersView: IView {

    }

    interface IComicChaptersPresenter: IPresenter<IComicChaptersView> {

        fun onCreate(comic: ComicViewModel)
        fun downloadChapters(chapters: List<ChapterViewModel>)

    }
}