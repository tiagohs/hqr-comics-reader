package com.tiagohs.hqr.dragger.modules

import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.sources.SourceManager
import com.tiagohs.hqr.sources.portuguese.HQBRSource
import com.tiagohs.hqr.ui.contracts.*
import com.tiagohs.hqr.ui.presenter.*
import dagger.Module
import dagger.Provides
import io.reactivex.disposables.CompositeDisposable

@Module
class PresenterModule {

    @Provides
    internal fun provideHomePresenter(comicsInterceptor: Contracts.IComicsInterceptor, compositeDisposable: CompositeDisposable, sourceManager: SourceManager, preferenceHelper: PreferenceHelper, sourceRepository: ISourceRepository): HomeContract.IHomePresenter {
        return HomePresenter(compositeDisposable, sourceManager, preferenceHelper, sourceRepository, comicsInterceptor)
    }

    @Provides
    internal fun provideReaderPresenter(compositeDisposable: CompositeDisposable, source: HQBRSource): ReaderContract.IReaderPresenter {
        return ReaderPresenter(compositeDisposable, source)
    }

    @Provides
    internal fun provideComicDetailsPresenter(compositeDisposable: CompositeDisposable, source: HQBRSource): ComicDetailsContract.IComicDetailsPresenter {
        return ComicDetailsPresenter(compositeDisposable, source)
    }

    @Provides
    internal fun provideListComicsPresenter(compositeDisposable: CompositeDisposable, source: HQBRSource): ListComicsContract.IListComicsPresenter {
        return ListComicsPresenter(compositeDisposable, source)
    }

    @Provides
    internal fun provideSearchPresenter(compositeDisposable: CompositeDisposable, source: HQBRSource): SearchContract.ISearchPresenter {
        return SearchPresenter(compositeDisposable, source)
    }

    @Provides
    internal fun provideSourcePresenter(sourceRepository: ISourceRepository): SourcesContract.ISourcesPresenter {
        return SourcesPresenter(sourceRepository)
    }
}