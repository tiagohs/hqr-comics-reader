package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.domain.repository.Backups
import com.tiagohs.hqr.domain.repository.DownloadItem
import com.tiagohs.hqr.domain.repository.DownloadSettings
import com.tiagohs.hqr.domain.repository.DownloadState
import com.tiagohs.hqr.domain.repository.Downloads
import com.tiagohs.hqr.domain.repository.RestoreFailure
import com.tiagohs.hqr.domain.repository.RestoreProblem
import com.tiagohs.hqr.domain.repository.RestoreSummary
import com.tiagohs.hqr.presentation.downloads.DownloadsViewModel
import com.tiagohs.hqr.presentation.settings.BackupViewModel
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DownloadsAndBackupViewModelTest : MainDispatcherTest() {
    private class FakeDownloads : Downloads {
        val items = MutableStateFlow<List<DownloadItem>>(emptyList())
        val calls = mutableListOf<String>()

        override fun observe() = items

        override suspend fun enqueue(chapterIds: List<Long>) {
            calls += "enqueue $chapterIds"
        }

        override suspend fun pauseAll() {
            calls += "pause"
        }

        override suspend fun resumeAll() {
            calls += "resume"
        }

        override suspend fun move(
            chapterId: Long,
            by: Int,
        ) {
            calls += "move $chapterId $by"
        }

        override suspend fun remove(chapterIds: Collection<Long>) {
            calls += "remove ${chapterIds.toList()}"
        }

        override suspend fun removeAll() {
            calls += "removeAll"
        }

        override suspend fun usedBytes() = items.value.count { it.state == DownloadState.DONE } * 1024L

        override suspend fun networkRuleChanged() {
            calls += "rule"
        }
    }

    private class FakeSettings : DownloadSettings {
        override val wifiOnly = MutableStateFlow(true)
        override val deleteAfterRead = MutableStateFlow(false)

        override fun setWifiOnly(on: Boolean) {
            wifiOnly.value = on
        }

        override fun setDeleteAfterRead(on: Boolean) {
            deleteAfterRead.value = on
        }
    }

    private fun item(
        id: Long,
        manga: Long,
        state: DownloadState,
    ) = DownloadItem(id, manga, "Obra $manga", "$id", state, 0, 10)

    @Test
    fun RF63_RF66_queueAndDownloadedApartWithSpaceAndOptions() =
        runTest {
            val downloads = FakeDownloads()
            val settings = FakeSettings()
            val vm = track(DownloadsViewModel(downloads, settings))
            downloads.items.value =
                listOf(item(1, 7, DownloadState.DONE), item(2, 7, DownloadState.DONE), item(3, 8, DownloadState.QUEUED))
            val state = vm.state.first { it.done.size == 2 && it.usedBytes == 2048L }
            state.queue.map { it.chapterId } shouldContainExactly listOf(3L)
            state.paused shouldBe false
            vm.pauseAll()
            vm.move(3, -1)
            vm.removeWork(7)
            vm.removeAll()
            vm.setDeleteAfterRead(true)
            vm.setWifiOnly(false)
            vm.state.first { it.deleteAfterRead && !it.wifiOnly }
            // RF-64: the new rule reaches the queue.
            waitFor { downloads.calls.size == 5 }
            downloads.calls[2] shouldBe "remove [1, 2]"
            downloads.calls[4] shouldBe "rule"
        }

    @Test
    fun RF72_RF73_backupCreatesSavesAndRestoreShowsSummaryOrProblem() =
        runTest {
            var fail: RestoreProblem? = null
            val backups =
                object : Backups {
                    override suspend fun create() = byteArrayOf(1, 2)

                    override suspend fun restore(bytes: ByteArray): Result<RestoreSummary> =
                        fail?.let { Result.failure(RestoreFailure(it)) } ?: Result.success(RestoreSummary(3, 10, 1, 0, listOf("HQ Now")))

                    override fun fileName() = "hqr.hqrbackup"
                }
            val vm = track(BackupViewModel(backups))
            vm.create()
            vm.state
                .first { it.pending != null }
                .pending!!
                .size shouldBe 2
            vm.saved(true)
            vm.state.value.saved shouldBe true
            vm.restore(byteArrayOf(1))
            vm.state
                .first { it.summary != null }
                .summary!!
                .unavailableSources shouldBe listOf("HQ Now")
            vm.dismiss()
            fail = RestoreProblem.NEWER_VERSION
            vm.restore(byteArrayOf(1))
            vm.state.first { it.problem != null }.problem shouldBe RestoreProblem.NEWER_VERSION
            vm.fileName shouldBe "hqr.hqrbackup"
        }
}
