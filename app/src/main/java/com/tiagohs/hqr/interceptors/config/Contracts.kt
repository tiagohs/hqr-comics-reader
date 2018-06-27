package com.tiagohs.hqr.interceptors.config

import com.tiagohs.hqr.models.viewModels.ComicViewModel
import io.reactivex.Observable


object Contracts {

    interface IBaseInterceptor {
        fun onUnbind()
        fun onBind()
    }

    interface ISourceInterceptor: IBaseInterceptor {

    }

    interface IComicsInterceptor: IBaseInterceptor {

        fun onGetLastestComics(): Observable<List<ComicViewModel>>
        fun onGetPopularComics(): Observable<List<ComicViewModel>>

        fun subscribeComicDetailSubject(): Observable<ComicViewModel>
    }

}