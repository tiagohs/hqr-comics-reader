package com.tiagohs.hqr.domain.repository

/** A saved answer and when it was saved, in epoch milliseconds. */
data class CachedText(
    val text: String,
    val savedAtEpochMs: Long,
)

/**
 * Answers kept on the device so a screen opens with what it had and asks the source again only when it is old
 * (RF-116, RF-117). The text is whatever the caller serialized; the store only keeps it with the time.
 */
interface CacheStore {
    fun read(key: String): CachedText?

    fun write(
        key: String,
        text: String,
    )

    fun remove(key: String)

    /** Nothing is kept: every screen asks the source (tests, platforms without storage). */
    object None : CacheStore {
        override fun read(key: String): CachedText? = null

        override fun write(
            key: String,
            text: String,
        ) = Unit

        override fun remove(key: String) = Unit
    }
}
