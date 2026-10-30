package com.tiagohs.hqr.domain.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Stable id of a source (RF-90): a constant in the code of each built-in source, a generated UUID for each OPDS
 * server. Saved works and chapters are keyed by it, so a value is never renamed.
 */
@Serializable
@JvmInline
value class SourceId(
    val value: String,
) {
    override fun toString(): String = value

    companion object {
        val LOCAL = SourceId("local")
        val MANGADEX = SourceId("mangadex")
        val HQ_NOW = SourceId("hqnow")
    }
}
