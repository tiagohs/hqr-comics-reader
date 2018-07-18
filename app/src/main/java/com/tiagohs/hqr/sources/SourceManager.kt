package com.tiagohs.hqr.sources

import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.sources.english.ReadComics
import com.tiagohs.hqr.sources.portuguese.HQBRSource
import okhttp3.OkHttpClient

class SourceManager(
        private val client: OkHttpClient,
        private val chapterCache: ChapterCache
) {

    private val mapOfSouces = mutableMapOf<Long, IHttpSource>()

    init {
        createSources().forEach { register(it) }
    }

    fun getHttpSouces() = mapOfSouces.values.filterIsInstance<HttpSourceBase>()

    fun get(sourceId: Long): IHttpSource? {
        return mapOfSouces.get(sourceId)
    }

    fun remove(sourceId: Long) {
        mapOfSouces.remove(sourceId)
    }

    private fun register(source: IHttpSource) {
        if (!mapOfSouces.containsKey(source.id))
            mapOfSouces.put(source.id, source)
    }

    private fun createSources(): List<IHttpSource> = listOf(
            HQBRSource(client, chapterCache),
            ReadComics(client, chapterCache)
    )
}