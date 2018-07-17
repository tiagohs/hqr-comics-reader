package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.models.view_models.ReaderChapterViewModel
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class ReaderContract {

    interface IReaderView: IView {

        fun onBindChapter(model: ReaderChapterViewModel, updateDataSet: Boolean)
        fun onPageDownloaded(page: Page)
    }

    interface IReaderPresenter: IPresenter<IReaderView> {

        fun onCreate()
        fun onGetChapterDetails(comicPath: String, chapterPath: String, updateDataSet: Boolean = false)
        fun onRequestNextChapter()

        fun onTrackUserHistory(page: Page?)
        fun onSaveUserHistory()
    }
}