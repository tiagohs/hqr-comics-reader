package com.tiagohs.hqr.dragger.modules

import com.tiagohs.hqr.sources.portuguese.HQBRSource
import com.tiagohs.hqr.ui.contracts.HomeContract
import com.tiagohs.hqr.ui.contracts.ReaderContract
import com.tiagohs.hqr.ui.presenter.HomePresenter
import com.tiagohs.hqr.ui.presenter.ReaderPresenter
import dagger.Module
import dagger.Provides
import io.reactivex.disposables.CompositeDisposable

@Module
class PresenterModule {

    @Provides
    internal fun provideHomePresenter(compositeDisposable: CompositeDisposable, source: HQBRSource): HomeContract.IHomePresenter {
        return HomePresenter(compositeDisposable, source)
    }

    @Provides
    internal fun provideReaderPresenter(compositeDisposable: CompositeDisposable, source: HQBRSource): ReaderContract.IReaderPresenter {
        return ReaderPresenter(compositeDisposable, source)
    }
}