package com.tiagohs.hqr.dragger.modules

import android.content.Context
import com.tiagohs.hqr.database.IChapterRepository
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.download.DownloadManager
import com.tiagohs.hqr.download.DownloadProvider
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.sources.SourceManager
import com.tiagohs.hqr.ui.contracts.*
import com.tiagohs.hqr.ui.presenter.*
import dagger.Module
import dagger.Provides

@Module
class PresenterModule {

    @Provides
    internal fun provideHomePresenter(homeInterceptor: Contracts.IHomeInterceptor, sourceManager: SourceManager, preferenceHelper: PreferenceHelper, sourceRepository: ISourceRepository, comicsRepository: IComicsRepository): HomeContract.IHomePresenter {
        return HomePresenter(sourceManager, preferenceHelper, sourceRepository, homeInterceptor, comicsRepository)
    }

    @Provides
    internal fun provideReaderPresenter(preferenceHelper: PreferenceHelper, sourceManager: SourceManager, comicsRepository: IComicsRepository, chapterRepository: IChapterRepository, downloadManager: DownloadManager, provider: DownloadProvider, context: Context): ReaderContract.IReaderPresenter {
        return ReaderPresenter(preferenceHelper, sourceManager, comicsRepository, chapterRepository, downloadManager, provider, context)
    }

    @Provides
    internal fun provideComicDetailsPresenter(interceptor: Contracts.IComicsDetailsInterceptor): ComicDetailsContract.IComicDetailsPresenter {
        return ComicDetailsPresenter(interceptor)
    }

    @Provides
    internal fun provideListComicsPresenter(interceptor: Contracts.IListComicsInterceptor, preferenceHelper: PreferenceHelper, comicsRepository: IComicsRepository): ListComicsContract.IListComicsPresenter {
        return ListComicsPresenter(interceptor, comicsRepository, preferenceHelper)
    }

    @Provides
    internal fun provideSearchPresenter(searchInterceptor: Contracts.ISearchInterceptor, preferenceHelper: PreferenceHelper, comicsRepository: IComicsRepository): SearchContract.ISearchPresenter {
        return SearchPresenter(searchInterceptor, comicsRepository, preferenceHelper)
    }

    @Provides
    internal fun provideSourcePresenter(sourceRepository: ISourceRepository): SourcesContract.ISourcesPresenter {
        return SourcesPresenter(sourceRepository)
    }

    @Provides
    internal fun provideComicChaptersPresenter(downloadManager: DownloadManager, chapterRepository: IChapterRepository): ComicChaptersContract.IComicChaptersPresenter{
        return ComicChaptersPresenter(downloadManager, chapterRepository)
    }

    @Provides
    internal fun provideDownloadManagerPresenter(downloadManager: DownloadManager): DownloadManagerContract.IDownloadManagerPresenter{
        return DownloadManagerPresenter(downloadManager)
    }

    @Provides
    internal fun provideFavoritesPresenter(favoritesInterceptor: Contracts.IFavoritesInterceptor, comicsRepository: IComicsRepository): FavoritesContract.IFavoritesPresenter {
        return FavoritesPresenter(favoritesInterceptor, comicsRepository)
    }

}