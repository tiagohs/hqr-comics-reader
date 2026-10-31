package com.tiagohs.hqr.ui.di

import com.tiagohs.hqr.presentation.color.CoverPixels
import com.tiagohs.hqr.ui.color.ImageCoverPixels
import org.koin.core.module.Module
import org.koin.dsl.module

/** What the Compose screens add to the shared graph (com.tiagohs.hqr.app): the cover color read with Compose's decoder. */
val uiModule: Module =
    module {
        single<CoverPixels> { ImageCoverPixels(get()) }
    }
