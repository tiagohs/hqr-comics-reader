package com.tiagohs.hqr.domain.model

/** Formats a chapter number the way people write it: 12, 10.5 (never 12.0). */
fun Double.chapterLabel(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
