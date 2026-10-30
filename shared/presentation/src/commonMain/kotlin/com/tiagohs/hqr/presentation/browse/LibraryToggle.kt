package com.tiagohs.hqr.presentation.browse

import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.LibraryRemoval
import com.tiagohs.hqr.domain.repository.MangaRepository

/** RF-52: a long press on a result puts the work in the library or takes it out; null means it went in. */
suspend fun MangaRepository.toggleInLibrary(manga: SourceManga): LibraryRemoval? {
    val saved = upsert(manga)
    if (saved.inLibrary) return removeFromLibrary(saved.id)
    setInLibrary(saved.id, true)
    return null
}

/** "Desfazer" for [toggleInLibrary]: restores what a removal took, or takes out what was just added. */
suspend fun MangaRepository.undoToggle(
    manga: SourceManga,
    removal: LibraryRemoval?,
) {
    if (removal != null) {
        restoreToLibrary(removal)
    } else {
        find(manga.sourceId, manga.key)?.let { removeFromLibrary(it.id) }
    }
}
