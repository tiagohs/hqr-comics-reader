package com.tiagohs.hqr.ui.presenter

import com.tiagohs.hqr.database.repository.ISourceRepository
import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.ui.contracts.SourcesContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.disposables.CompositeDisposable
import io.realm.RealmResults

class SourcesPresenter(
        val sourcesRepository: ISourceRepository
): BasePresenter<SourcesContract.ISourcesView>(CompositeDisposable()), SourcesContract.ISourcesPresenter {

    override fun getAllSources() {

        sourcesRepository.getSourcesAndLanguages()
                .addChangeListener { sources: RealmResults<CatalogueSource>? ->
                    if (sources!!.isNotEmpty()) {
                        mView!!.onBindSources(sources.toList())
                    }
                }

    }
}