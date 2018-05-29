package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class ReaderContract {

    interface IReaderView: IView {

        fun onBindChapter(ch: Chapter?)
    }

    interface IReaderPresenter: IPresenter<IReaderView> {

        fun onGetChapterDetails(chapterPath: String)
    }
}