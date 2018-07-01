package com.tiagohs.hqr.dragger.modules

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.interceptors.ComicsDetailsInterceptor
import com.tiagohs.hqr.interceptors.HomeInterceptor
import com.tiagohs.hqr.interceptors.ListComicsInterceptor
import com.tiagohs.hqr.interceptors.SearchInterceptor
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.sources.SourceManager
import dagger.Module
import dagger.Provides

@Module
class InterceptorModule {

    @Provides
    internal fun providerComicsInterceptor(preferenceHelper: PreferenceHelper, comicsRepository: IComicsRepository, sourceRepository: ISourceRepository, sourceManager: SourceManager): Contracts.IHomeInterceptor {
        return HomeInterceptor(preferenceHelper, comicsRepository, sourceRepository, sourceManager)
    }

    @Provides
    internal fun providerListComicsInterceptor(preferenceHelper: PreferenceHelper, comicsRepository: IComicsRepository, sourceRepository: ISourceRepository, sourceManager: SourceManager): Contracts.IListComicsInterceptor {
        return ListComicsInterceptor(preferenceHelper, comicsRepository, sourceRepository, sourceManager)
    }

    @Provides
    internal fun providerComicsDetailsInterceptor(preferenceHelper: PreferenceHelper, comicsRepository: IComicsRepository, sourceRepository: ISourceRepository, sourceManager: SourceManager): Contracts.IComicsDetailsInterceptor {
        return ComicsDetailsInterceptor(preferenceHelper, comicsRepository, sourceRepository, sourceManager)
    }

    @Provides
    internal fun providerSearchInterceptor(preferenceHelper: PreferenceHelper, comicsRepository: IComicsRepository, sourceRepository: ISourceRepository, sourceManager: SourceManager): Contracts.ISearchInterceptor {
        return SearchInterceptor(preferenceHelper, comicsRepository, sourceRepository, sourceManager)
    }
}