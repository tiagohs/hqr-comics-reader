package com.tiagohs.hqr.ui.callbacks

import com.tiagohs.hqr.models.database.SourceDB


interface ISourcesCallback {

    fun onSourceSelect(sourceDB: SourceDB)
}