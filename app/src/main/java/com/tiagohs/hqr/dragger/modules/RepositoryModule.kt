package com.tiagohs.hqr.dragger.modules

import com.tiagohs.hqr.database.*
import com.tiagohs.hqr.database.repository.*
import dagger.Module
import dagger.Provides

@Module
class RepositoryModule {

    @Provides
    fun providerSourceRepository(): ISourceRepository {
        return SourceRepository()
    }

    @Provides
    fun providerChapterRepository(): IChapterRepository {
        return ChapterRepository()
    }

    @Provides
    fun providerComicRepository(sourceRepository: ISourceRepository): IComicsRepository {
        return ComicsRepository(sourceRepository)
    }

    @Provides
    fun providerHistoryRepository(): IHistoryRepository {
        return HistoryRepository()
    }

    @Provides
    fun providerDefaultModelsRepository(sourceRepository: ISourceRepository): IDefaultModelsRepository {
        return DefaultModelsRepository(sourceRepository)
    }

}