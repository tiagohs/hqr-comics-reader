package com.tiagohs.hqr.interceptors

import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.interceptors.config.BaseInterceptor
import com.tiagohs.hqr.interceptors.config.Contracts

class SourceInterceptor(
        private val sourceRepository: ISourceRepository
): BaseInterceptor(), Contracts.ISourceInterceptor {

}