package com.tiagohs.hqr.ui.reader

private fun switchFullscreen(): Boolean =
    js(
        """{
        if (document.fullscreenElement) { document.exitFullscreen(); }
        else if (document.documentElement.requestFullscreen) { document.documentElement.requestFullscreen(); }
        else { return false; }
        return true;
    }""",
    )

actual fun toggleFullscreen(): Boolean = switchFullscreen()
