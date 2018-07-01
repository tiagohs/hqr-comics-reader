package com.tiagohs.hqr.dragger.modules

import com.tiagohs.hqr.database.IChapterRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.download.DownloadManager
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.sources.SourceManager
import com.tiagohs.hqr.sources.portuguese.HQBRSource
import com.tiagohs.hqr.ui.contracts.*
import com.tiagohs.hqr.ui.presenter.*
import dagger.Module
import dagger.Provides

@Module
class PresenterModule {

    @Provides
    internal fun provideHomePresenter(homeInterceptor: Contracts.IHomeInterceptor, sourceManager: SourceManager, preferenceHelper: PreferenceHelper, sourceRepository: ISourceRepository): HomeContract.IHomePresenter {
        return HomePresenter(sourceManager, preferenceHelper, sourceRepository, homeInterceptor)
    }

    @Provides
    internal fun provideReaderPresenter(source: HQBRSource): ReaderContract.IReaderPresenter {
        return ReaderPresenter(source)
    }

    @Provides
    internal fun provideComicDetailsPresenter(interceptor: Contracts.IComicsDetailsInterceptor): ComicDetailsContract.IComicDetailsPresenter {
        return ComicDetailsPresenter(interceptor)
    }

    @Provides
    internal fun provideListComicsPresenter(interceptor: Contracts.IListComicsInterceptor): ListComicsContract.IListComicsPresenter {
        return ListComicsPresenter(interceptor)
    }

    @Provides
    internal fun provideSearchPresenter(searchInterceptor: Contracts.ISearchInterceptor): SearchContract.ISearchPresenter {
        return SearchPresenter(searchInterceptor)
    }

    @Provides
    internal fun provideSourcePresenter(sourceRepository: ISourceRepository): SourcesContract.ISourcesPresenter {
        return SourcesPresenter(sourceRepository)
    }

    @Provides
    internal fun provideComicChaptersPresenter(downloadManager: DownloadManager, chapterRepository: IChapterRepository): ComicChaptersContract.IComicChaptersPresenter{
        return ComicChaptersPresenter(downloadManager, chapterRepository)
    }
}