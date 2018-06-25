package com.tiagohs.hqr.dragger.modules

import com.tiagohs.hqr.database.IChapterRepository
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.IHistoryRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.database.repository.ChapterRepository
import com.tiagohs.hqr.database.repository.ComicsRepository
import com.tiagohs.hqr.database.repository.HistoryRepository
import com.tiagohs.hqr.database.repository.SourceRepository
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
    fun providerComicRepository(): IComicsRepository {
        return ComicsRepository()
    }

    @Provides
    fun providerHistoryRepository(): IHistoryRepository {
        return HistoryRepository()
    }

}