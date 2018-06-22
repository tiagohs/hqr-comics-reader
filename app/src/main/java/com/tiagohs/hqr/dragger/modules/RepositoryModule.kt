package com.tiagohs.hqr.dragger.modules

import com.tiagohs.hqr.database.repository.ISourceRepository
import com.tiagohs.hqr.database.repository.SourceRepository
import dagger.Module
import dagger.Provides

@Module
class RepositoryModule {

    @Provides
    fun providerSourceRepository(): ISourceRepository {
        return SourceRepository()
    }

}