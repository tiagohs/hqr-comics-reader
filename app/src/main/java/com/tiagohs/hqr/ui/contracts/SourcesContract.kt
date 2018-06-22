package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class SourcesContract {

    interface ISourcesView: IView {

        fun onBindSources(sources: List<CatalogueSource>)
    }

    interface ISourcesPresenter: IPresenter<ISourcesView> {

        fun getAllSources()
    }
}