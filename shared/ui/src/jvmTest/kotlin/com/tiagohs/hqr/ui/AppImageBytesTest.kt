package com.tiagohs.hqr.ui

import com.tiagohs.hqr.data.net.MemoryImageCache
import com.tiagohs.hqr.data.net.hqrHttpClient
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.ImageSource
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.ui.di.AppImageBytes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class AppImageBytesTest {
    private class Gate : ImageSource {
        var calls = 0
        var open = CompletableDeferred<Unit>()

        override suspend fun image(
            url: String,
            headers: Map<String, String>,
        ): ByteArray {
            calls++
            open.await()
            return byteArrayOf(7)
        }
    }

    private val demo = DemoSource()
    private val gate = Gate()

    private fun images(): AppImageBytes {
        val wrapped =
            object : com.tiagohs.hqr.domain.source.CatalogSource by demo, ImageSource by gate {}
        return AppImageBytes(SourceRegistry(listOf(wrapped)), hqrHttpClient("test"), MemoryImageCache())
    }

    @Test
    fun RF41_pageLoadedOnceWhenPagerAndPrefetchAsk() =
        runTest {
            val bytes = images()
            val a = async { bytes.load(SourceId("demo"), "u1", emptyMap()) }
            val b = async { bytes.load(SourceId("demo"), "u1", emptyMap()) }
            testScheduler.advanceUntilIdle()
            gate.open.complete(Unit)
            assertContentEquals(byteArrayOf(7), a.await())
            assertContentEquals(byteArrayOf(7), b.await())
            assertEquals(1, gate.calls)
        }

    @Test
    fun RF41_waiterGetsThePageWhenTheFirstCallerIsCancelled() =
        runTest {
            val bytes = images()
            val prefetch = async { bytes.load(SourceId("demo"), "u2", emptyMap()) }
            testScheduler.advanceUntilIdle()
            val visible = async { bytes.load(SourceId("demo"), "u2", emptyMap()) }
            testScheduler.advanceUntilIdle()
            prefetch.cancel()
            testScheduler.advanceUntilIdle()
            gate.open.complete(Unit)
            assertContentEquals(byteArrayOf(7), visible.await())
            assertEquals(2, gate.calls)
        }
}
