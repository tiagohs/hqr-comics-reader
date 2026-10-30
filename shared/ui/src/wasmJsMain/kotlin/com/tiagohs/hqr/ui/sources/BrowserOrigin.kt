package com.tiagohs.hqr.ui.sources

import kotlinx.browser.window

actual fun browserOrigin(): String? = window.location.origin
