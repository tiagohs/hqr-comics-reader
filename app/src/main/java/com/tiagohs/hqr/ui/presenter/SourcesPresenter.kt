package com.tiagohs.hqr.ui.presenter

import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.ui.contracts.SourcesContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SourcesPresenter(
        val sourcesRepository: ISourceRepository
): BasePresenter<SourcesContract.ISourcesView>(), SourcesContract.ISourcesPresenter {

    override fun getAllSources() {

        sourcesRepository.getAllCatalogueSources()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{ sources: List<CatalogueSource>? ->
                    if (sources != null) {
                        mView!!.onBindSources(sources.toList())
                    }
                }
    }

}