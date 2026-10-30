package com.tiagohs.hqr.domain.model

/** A work saved on the device: in the library or only seen (history, progress, downloads). */
data class Manga(
    val id: Long,
    val source: SourceManga,
    val inLibrary: Boolean,
    val addedAtEpochMs: Long?,
    /** Seed color extracted from the cover (RF-98); null means the violet base. */
    val coverColor: Int? = null,
) {
    val sourceId: SourceId get() = source.sourceId
    val key: String get() = source.key
    val title: String get() = source.title
}

/** Reading state of one chapter (RF-30). */
data class Chapter(
    val id: Long,
    val mangaId: Long,
    val source: SourceChapter,
    val read: Boolean,
    val lastPage: Int,
    val pageCount: Int?,
    val removedAtSource: Boolean = false,
    val downloaded: Boolean = false,
    /** When HQR first saw the chapter at the source: "new" since the last opening of the work (RF-33). */
    val fetchedAtEpochMs: Long = 0,
) {
    val number: Double get() = source.number
    val inProgress: Boolean get() = !read && lastPage > 0
}

/** A library category (RF-53). [id] 0 is "Sem categoria" and can't be deleted or renamed. */
data class Category(
    val id: Long,
    val name: String,
    val order: Int,
) {
    val isDefault: Boolean get() = id == DEFAULT_ID

    companion object {
        const val DEFAULT_ID = 0L
    }
}
