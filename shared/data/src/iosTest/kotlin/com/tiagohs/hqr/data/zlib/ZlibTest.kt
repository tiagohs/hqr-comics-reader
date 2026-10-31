package com.tiagohs.hqr.data.zlib

import com.tiagohs.hqr.data.backup.gunzip
import com.tiagohs.hqr.data.backup.gzip
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZlibTest {
    private val text = ByteArray(300_000) { (it % 7 + 'a'.code).toByte() }

    @Test
    fun RF75_backup_round_trips_through_gzip() {
        assertContentEquals(text, gunzip(gzip(text)))
        assertContentEquals(ByteArray(0), gunzip(gzip(ByteArray(0))))
    }

    @Test
    fun RF75_a_cut_backup_is_refused() {
        val packed = gzip(text)
        assertNull(Zlib.gunzip(packed.copyOf(packed.size / 2)))
    }

    @Test
    fun RNF26_raw_inflate_stops_at_the_declared_size() {
        // A gzip stream minus its 10-byte header and 8-byte trailer is raw DEFLATE.
        val raw = gzip(text).let { it.copyOfRange(10, it.size - 8) }
        assertContentEquals(text, Zlib.inflateRaw(raw, text.size))
        assertEquals(1000, Zlib.inflateRaw(raw, 1000)?.size)
    }
}
