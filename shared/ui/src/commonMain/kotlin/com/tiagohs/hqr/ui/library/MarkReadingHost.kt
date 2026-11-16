package com.tiagohs.hqr.ui.library

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Which work's sheet is open; the screens that offer "Marcar leitura" hold one (RF-126). */
@Stable
class MarkReadingHost {
    var mangaId by mutableStateOf<Long?>(null)
        private set

    fun open(id: Long) {
        mangaId = id
    }

    fun close() {
        mangaId = null
    }
}
