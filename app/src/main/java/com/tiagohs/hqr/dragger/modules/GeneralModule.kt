package com.tiagohs.hqr.dragger.modules

import dagger.Module
import dagger.Provides
import io.reactivex.disposables.CompositeDisposable

@Module
class GeneralModule {

    @Provides
    internal fun providesCompositeDisposable(): CompositeDisposable {
        return CompositeDisposable()
    }
}