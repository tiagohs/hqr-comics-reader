package com.tiagohs.hqr.dragger.modules

import android.content.Context
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import dagger.Module
import dagger.Provides
import io.reactivex.disposables.CompositeDisposable

@Module
class GeneralModule {

    @Provides
    internal fun providesCompositeDisposable(): CompositeDisposable {
        return CompositeDisposable()
    }

    @Provides
    internal fun providePreferencesHelper(context: Context): PreferenceHelper {
        return PreferenceHelper(context)
    }

}