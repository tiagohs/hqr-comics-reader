package com.tiagohs.hqr.dragger.components

import com.tiagohs.hqr.dragger.modules.*
import com.tiagohs.hqr.dragger.scopes.PerFragment
import com.tiagohs.hqr.ui.views.activities.ComicDetailsActivity
import com.tiagohs.hqr.ui.views.activities.ReaderActivity
import com.tiagohs.hqr.ui.views.fragments.HomeFragment
import dagger.Component

@PerFragment
@Component(modules = arrayOf(
        AppModule::class,
        NetModule::class,
        PresenterModule::class,
        GeneralModule::class,
        SourcesModule::class
        )
)
interface HQRComponent {

    fun inject(homeFragment: HomeFragment)
    fun inject(readerActivity: ReaderActivity)
    fun inject(comicDetailsActivity: ComicDetailsActivity)
}