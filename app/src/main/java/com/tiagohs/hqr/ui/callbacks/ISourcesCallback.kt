package com.tiagohs.hqr.ui.callbacks

import com.tiagohs.hqr.models.database.Source


interface ISourcesCallback {

    fun onSourceSelect(source: Source)
}