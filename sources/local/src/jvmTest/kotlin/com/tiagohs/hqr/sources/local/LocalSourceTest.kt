package com.tiagohs.hqr.sources.local

import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.source.SourceException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test

class LocalSourceTest {
    private val fs = FakeFileSystem()
    private val root = "/hqs".toPath()
    private val source = LocalSource(OkioFileTree(fs, root))

    private fun cbz(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun write(
        path: String,
        bytes: ByteArray,
    ) {
        val full = root / path
        full.parent?.let { fs.createDirectories(it) }
        fs.write(full) { write(bytes) }
    }

    @Test
    fun RF13_rootFoldersAndArchivesAreWorks() =
        runTest {
            write("Invencível/Ed 2.cbz", cbz("01.jpg" to byteArrayOf(1)))
            write("Kaiju Express vol. 1.cbz", cbz("01.jpg" to byteArrayOf(1)))
            write("notas.txt", byteArrayOf(1))
            source.popular(1).items.map { it.title } shouldContainExactly listOf("Invencível", "Kaiju Express vol. 1")
        }

    @Test
    fun RF13_archivesInAWorkAreChaptersInNaturalOrder() =
        runTest {
            write("Inv/Ed 10.cbz", cbz("a.jpg" to byteArrayOf(1)))
            write("Inv/Ed 2.cbz", cbz("a.jpg" to byteArrayOf(1)))
            val chapters = source.chapters(SourceManga(SourceId.LOCAL, "Inv", "Inv"))
            chapters.map { it.number } shouldContainExactly listOf(2.0, 10.0)
        }

    @Test
    fun RF13_cbzPagesFollowNaturalOrderAndLoad() =
        runTest {
            val page2 = kotlin.random.Random(4).nextBytes(5000)
            write(
                "Inv/Ed 1.cbz",
                cbz(
                    "p10.jpg" to byteArrayOf(10),
                    "p2.jpg" to page2,
                    "__MACOSX/._p2.jpg" to byteArrayOf(0),
                    "info.txt" to byteArrayOf(0),
                ),
            )
            val manga = SourceManga(SourceId.LOCAL, "Inv", "Inv")
            val chapter = source.chapters(manga).single()
            val pages = source.pages(manga, chapter)
            pages.map { it.imageUrl.substringAfter('!') } shouldContainExactly listOf("p2.jpg", "p10.jpg")
            source.load(pages.first().imageUrl) shouldBe page2
        }

    @Test
    fun RF13_folderOfImagesIsASingleChapterAndTheFirstIsTheCover() =
        runTest {
            write("Gato/1.png", byteArrayOf(1))
            write("Gato/2.png", byteArrayOf(2))
            source.chapters(SourceManga(SourceId.LOCAL, "Gato", "Gato")).size shouldBe 1
            source.load(LocalSource.coverUrl("Gato")) shouldBe byteArrayOf(1)
        }

    @Test
    fun RNF26_compressionBombIsRefused() =
        runTest {
            write("Bomb/1.cbz", cbz("p.jpg" to ByteArray(4_000_000)))
            val manga = SourceManga(SourceId.LOCAL, "Bomb", "Bomb")
            val ref = source.pages(manga, source.chapters(manga).single()).single().imageUrl
            shouldThrow<SourceException> { source.load(ref) }
        }

    @Test
    fun RF13_namesWithBangAndHashStillLoad() =
        runTest {
            val bytes = kotlin.random.Random(7).nextBytes(3000)
            write("Yotsuba&!/Vol #1!.cbz", cbz("p!1.jpg" to bytes))
            val manga = SourceManga(SourceId.LOCAL, "Yotsuba&!", "Yotsuba&!")
            val ref = source.pages(manga, source.chapters(manga).single()).single().imageUrl
            source.load(ref) shouldBe bytes
        }

    @Test
    fun RNF26_ratioAboveOneHundredIsRefused() =
        runTest {
            write("Bomb/1.cbz", cbz("p.jpg" to ByteArray(200_000)))
            val manga = SourceManga(SourceId.LOCAL, "Bomb", "Bomb")
            val ref = source.pages(manga, source.chapters(manga).single()).single().imageUrl
            shouldThrow<SourceException> { source.load(ref) }.kind shouldBe SourceException.Kind.PARSE
        }

    @Test
    fun RNF26_truncatedOrLyingArchivesAreRefused() =
        runTest {
            val good = cbz("p.jpg" to kotlin.random.Random(1).nextBytes(100))
            // Central directory says the entry is bigger than the whole file.
            val lying = good.copyOf()
            val cd =
                lying.indices.last { i ->
                    i + 3 < lying.size &&
                        lying[i] == 0x50.toByte() &&
                        lying[i + 1] == 0x4b.toByte() &&
                        lying[i + 2] == 1.toByte() &&
                        lying[i + 3] == 2.toByte()
                }
            lying[cd + 20] = 0x7f
            lying[cd + 23] = 0x7f
            write("L/1.cbz", lying)
            shouldThrow<SourceException> { ZipReader(OkioFileTree(fs, root), "L/1.cbz").entries() }
            // Truncated: the end record cut off.
            write("T/1.cbz", good.copyOf(good.size - 30))
            shouldThrow<SourceException> { ZipReader(OkioFileTree(fs, root), "T/1.cbz").entries() }
        }

    @Test
    fun RNF26_pathsWithDotDotAreIgnored() {
        ZipReader.isSafe("../evil.jpg") shouldBe false
        ZipReader.isSafe("a/../../b.jpg") shouldBe false
        ZipReader.isSafe("/etc/passwd") shouldBe false
        ZipReader.isSafe("pages/01.jpg") shouldBe true
    }

    @Test
    fun RF16_sourceNeverWrites() =
        runTest {
            write("A/1.png", byteArrayOf(1))
            val before = fs.allPaths.toList()
            source.popular(1)
            source.chapters(SourceManga(SourceId.LOCAL, "A", "A"))
            fs.allPaths.toList() shouldBe before
        }

    @Test
    fun RF13_noFolderChosenIsAClearError() =
        runTest {
            shouldThrow<SourceException> { LocalSource(OkioFileTree(fs, "/nada".toPath())).popular(1) }
        }

    @Test
    fun RF13_chapterNumbersComeFromTheName() {
        LocalSource.numberFrom("Cap. 12.cbz") shouldBe 12.0
        LocalSource.numberFrom("Edição 10.5") shouldBe 10.5
        LocalSource.numberFrom("vol 3") shouldBe 3.0
    }
}
