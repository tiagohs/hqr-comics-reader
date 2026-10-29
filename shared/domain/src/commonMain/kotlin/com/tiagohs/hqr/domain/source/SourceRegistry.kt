package com.tiagohs.hqr.domain.source

import com.tiagohs.hqr.domain.model.SourceId

/** The sources this build carries (RF-02, ADR-003), plus the user's servers registered at runtime (RF-17). */
class SourceRegistry(
    builtIn: List<Source>,
) {
    private val sources = LinkedHashMap<SourceId, Source>().apply { builtIn.forEach { put(it.id, it) } }

    val all: List<Source> get() = sources.values.toList()

    operator fun get(id: SourceId): Source? = sources[id]

    fun require(id: SourceId): Source = sources[id] ?: throw SourceException("Fonte indisponível: $id", SourceException.Kind.UNAVAILABLE)

    fun register(source: Source) {
        sources[source.id] = source
    }

    fun unregister(id: SourceId) {
        sources.remove(id)
    }
}

/** Bytes of a page or cover, from the cache, the downloads or the source (ADR-006). */
fun interface ImageBytes {
    suspend fun load(
        sourceId: SourceId,
        url: String,
        headers: Map<String, String>,
    ): ByteArray
}
