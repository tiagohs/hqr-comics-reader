package com.tiagohs.hqr.dragger.modules

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.interceptors.ComicsInterceptor
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.sources.SourceManager
import dagger.Module
import dagger.Provides

@Module
class InterceptorModule {

    @Provides
    internal fun providerComicsInterceptor(preferenceHelper: PreferenceHelper, comicsRepository: IComicsRepository, sourceRepository: ISourceRepository, sourceManager: SourceManager): Contracts.IComicsInterceptor {
        return ComicsInterceptor(preferenceHelper, comicsRepository, sourceRepository, sourceManager)
    }
}