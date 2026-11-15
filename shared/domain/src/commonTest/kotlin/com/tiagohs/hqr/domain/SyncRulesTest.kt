package com.tiagohs.hqr.domain

import com.tiagohs.hqr.domain.repository.SyncError
import com.tiagohs.hqr.domain.repository.SyncStatus
import com.tiagohs.hqr.domain.sync.ChapterRecord
import com.tiagohs.hqr.domain.sync.SyncChange
import com.tiagohs.hqr.domain.sync.SyncItemId
import com.tiagohs.hqr.domain.sync.SyncKeys
import com.tiagohs.hqr.domain.sync.SyncKind
import com.tiagohs.hqr.domain.sync.SyncRules
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SyncRulesTest {
    private val manga = SyncItemId(SyncKind.MANGA, SyncKeys.join("mangadex", "abc"))
    private val other = SyncItemId(SyncKind.MANGA, SyncKeys.join("mangadex", "xyz"))
    private val chapter = SyncItemId(SyncKind.CHAPTER, SyncKeys.join("mangadex", "abc", "c12"))
    private val category = SyncItemId(SyncKind.CATEGORY, "lendo")

    @Test
    fun RF121_diff_sends_new_and_changed_items_and_tombstones_for_removed_ones() {
        val changes =
            SyncRules.diff(
                snapshot = mapOf(manga to "a", category to "c", chapter to "x"),
                current = mapOf(manga to "b", other to "o"),
                now = 500,
                initial = false,
            )
        changes shouldContainExactlyInAnyOrder
            listOf(
                SyncChange(SyncKind.MANGA, manga.key, "b", 500, false),
                SyncChange(SyncKind.MANGA, other.key, "o", 500, false),
                SyncChange(SyncKind.CATEGORY, "lendo", "{}", 500, true),
            )
    }

    @Test
    fun RF124_before_the_first_sync_the_account_wins_except_for_chapters() {
        val changes = SyncRules.diff(emptyMap(), mapOf(manga to "m", chapter to "c"), now = 500, initial = true)
        changes.first { it.kind == SyncKind.MANGA }.changedAt shouldBe SyncRules.BEFORE_ACCOUNT
        changes.first { it.kind == SyncKind.CHAPTER }.changedAt shouldBe 500
    }

    @Test
    fun RF121_unchanged_items_send_nothing() {
        SyncRules.diff(mapOf(manga to "a"), mapOf(manga to "a"), now = 1, initial = false) shouldBe emptyList()
    }

    @Test
    fun RF123_read_on_the_web_and_opened_offline_on_the_phone_stays_read() {
        val web = ChapterRecord(number = 12.0, read = true, page = 21, pageCount = 22, progressAt = 100, readAt = 100)
        val phone = ChapterRecord(number = 12.0, read = false, page = 3, pageCount = 22, progressAt = 200, readAt = 200)
        val merged = SyncRules.mergeChapter(local = phone, remote = web)
        merged.read shouldBe true
        merged.page shouldBe 3
        merged.readAt shouldBe 200
        SyncRules.mergeChapter(local = web, remote = phone) shouldBe merged
    }

    @Test
    fun RF123_unread_by_the_user_wins_when_it_is_newer() {
        val read = ChapterRecord(number = 1.0, read = true, page = 9, progressAt = 100, readAt = 100)
        val unread = SyncRules.withUnreadMark(read, ChapterRecord(number = 1.0, read = false, page = 0, progressAt = 300, readAt = 100))
        unread.unreadAt shouldBe 300
        SyncRules.mergeChapter(local = read, remote = unread).read shouldBe false
        SyncRules.mergeChapter(local = unread, remote = read).read shouldBe false
    }

    @Test
    fun RF123_reading_after_the_unread_mark_counts_again() {
        val unread = ChapterRecord(number = 1.0, read = false, progressAt = 300, unreadAt = 300)
        val readLater = ChapterRecord(number = 1.0, read = true, page = 9, progressAt = 400, readAt = 400)
        val merged = SyncRules.mergeChapter(local = unread, remote = readLater)
        merged.read shouldBe true
        merged.unreadAt shouldBe null
    }

    @Test
    fun RF123_unread_mark_is_kept_while_the_chapter_stays_unread_and_dropped_when_read() {
        val marked = ChapterRecord(number = 1.0, read = false, progressAt = 300, unreadAt = 300)
        SyncRules.withUnreadMark(marked, marked.copy(page = 2, progressAt = 350)).unreadAt shouldBe 300
        SyncRules.withUnreadMark(marked, marked.copy(read = true, progressAt = 400)).unreadAt shouldBe null
        SyncRules.withUnreadMark(null, ChapterRecord(number = 1.0, page = 2, progressAt = 10)).unreadAt shouldBe null
    }

    @Test
    fun RF123_a_new_chapter_from_the_account_comes_as_it_is() {
        val remote = ChapterRecord(number = 3.0, read = true, progressAt = 5)
        SyncRules.mergeChapter(null, remote) shouldBe remote
    }

    @Test
    fun RF125_library_notice_only_after_a_day_of_failures() {
        val day = 86_400_000L
        SyncStatus(lastSuccessAt = 0, error = SyncError.OFFLINE).staleDays(now = day - 1) shouldBe null
        SyncStatus(lastSuccessAt = 0, error = SyncError.OFFLINE).staleDays(now = 2 * day + 5) shouldBe 2
        SyncStatus(lastSuccessAt = 0, error = null).staleDays(now = 5 * day) shouldBe null
        SyncStatus(lastSuccessAt = null, error = SyncError.SERVER).staleDays(now = 5 * day) shouldBe null
    }

    @Test
    fun RF121_kinds_round_trip_by_wire_name() {
        SyncKind.entries.forEach { SyncKind.of(it.wire) shouldBe it }
        SyncKind.of("download") shouldBe null
        SyncKeys.split(SyncKeys.join("a/b", "c#d")) shouldBe listOf("a/b", "c#d")
    }
}
