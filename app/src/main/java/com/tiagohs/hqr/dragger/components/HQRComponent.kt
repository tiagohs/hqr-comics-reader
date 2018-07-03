package com.tiagohs.hqr.dragger.components

import com.tiagohs.hqr.download.DownloaderService
import com.tiagohs.hqr.dragger.modules.*
import com.tiagohs.hqr.ui.adapters.SourceSection
import com.tiagohs.hqr.ui.views.activities.ComicDetailsActivity
import com.tiagohs.hqr.ui.views.activities.ReaderActivity
import com.tiagohs.hqr.ui.views.activities.SearchActivity
import com.tiagohs.hqr.ui.views.activities.SourcesActivity
import com.tiagohs.hqr.ui.views.fragments.*
import dagger.Component
import javax.inject.Singleton


@Component(modules = arrayOf(
        AppModule::class,
        NetModule::class,
        PresenterModule::class,
        GeneralModule::class,
        SourcesModule::class,
        CacheModule::class,
        DownloadModule::class,
        NotificationModule::class,
        SourcesModule::class,
        RepositoryModule::class,
        InterceptorModule::class
        )
)
@Singleton
interface HQRComponent {

    fun inject(homeFragment: HomeFragment)
    fun inject(readerActivity: ReaderActivity)
    fun inject(comicDetailsActivity: ComicDetailsActivity)
    fun inject(listComicsFragment: ListComicsFragment)
    fun inject(searchActivity: SearchActivity)
    fun inject(sourcesActivity: SourcesActivity)
    fun inject(settingsMainFragment: SettingsMainFragment)
    fun inject(comicChaptersFragment: ComicChaptersFragment)
    fun inject(downloadManagerFragment: DownloadManagerFragment)

    fun inject(downloaderService: DownloaderService)
    fun inject(sourceSection: SourceSection)
}