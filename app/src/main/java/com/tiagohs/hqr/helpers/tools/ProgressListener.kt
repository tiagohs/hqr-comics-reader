package com.tiagohs.hqr.helpers.tools


interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}
