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
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test

/** CBR and CB7 on the JVM libraries of Android (RF-13, ADR-011) and ComicInfo.xml (RF-14). */
class ArchivesAndComicInfoTest {
    private val fs = FakeFileSystem()
    private val root = "/hqs".toPath()
    private val tree = OkioFileTree(fs, root)
    private val temp = createTempDirectory("hqr-archives").toFile()
    private val cache = File(temp, "unpacked")
    private val unpacker = JvmArchiveUnpacker(tree, cache, keep = 2)
    private val source = LocalSource(tree, archives = unpacker)

    @AfterTest
    fun clean() {
        temp.deleteRecursively()
    }

    private fun write(
        path: String,
        bytes: ByteArray,
    ) {
        val full = root / path
        full.parent?.let { fs.createDirectories(it) }
        fs.write(full) { write(bytes) }
    }

    private fun fixture(name: String) = javaClass.getResourceAsStream("/archives/$name")!!.readBytes()

    private fun sevenZ(vararg entries: Pair<String, ByteArray>): ByteArray {
        val file = File(temp, "build.7z")
        SevenZOutputFile(file).use { out ->
            entries.forEach { (name, bytes) ->
                out.putArchiveEntry(out.createArchiveEntry(File(temp, "x"), name).apply { size = bytes.size.toLong() })
                out.write(bytes)
                out.closeArchiveEntry()
            }
        }
        return file.readBytes().also { file.delete() }
    }

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

    @Test
    fun RF13_rar4AndSolidRar5Unpack() =
        runTest {
            write("rar4.cbr", fixture("rar4.rar"))
            write("rar5.cbr", fixture("rar5-solid.rar"))
            unpacker.names("rar4.cbr") shouldContainExactly listOf("FILE1.TXT", "FILE2.TXT")
            unpacker.read("rar4.cbr", "FILE2.TXT").decodeToString().trim() shouldBe "file2"
            unpacker.names("rar5.cbr") shouldContainExactly (1..9).map { "file$it.txt" }
            unpacker.read("rar5.cbr", "file9.txt").decodeToString().isNotEmpty() shouldBe true
        }

    @Test
    fun RF13_cb7ChaptersReadInNaturalOrderWithACover() =
        runTest {
            write("Saga/Saga 02.cb7", sevenZ("p10.jpg" to byteArrayOf(10), "p2.jpg" to byteArrayOf(2), "notes.txt" to byteArrayOf(0)))
            write("Saga/Saga 01.cb7", sevenZ("a.png" to byteArrayOf(1)))
            val manga = SourceManga(SourceId.LOCAL, "Saga", "Saga")
            val chapters = source.chapters(manga)
            chapters.map { it.number } shouldContainExactly listOf(1.0, 2.0)
            val pages = source.pages(manga, chapters[1])
            pages.map { it.imageUrl.substringAfter('!') } shouldContainExactly listOf("p2.jpg", "p10.jpg")
            source.load(pages[1].imageUrl) shouldBe byteArrayOf(10)
            source.load(LocalSource.coverUrl("Saga")) shouldBe byteArrayOf(1)
            // The cover stays even after its archive leaves the unpacked ones.
            write("x.cb7", sevenZ("x.jpg" to byteArrayOf(7)))
            write("y.cb7", sevenZ("y.jpg" to byteArrayOf(8)))
            source.load(LocalSource.coverUrl("x.cb7")) shouldBe byteArrayOf(7)
            source.load(LocalSource.coverUrl("y.cb7")) shouldBe byteArrayOf(8)
            cache.listFiles()!!.count { it.isDirectory } shouldBe 2
            source.load(LocalSource.coverUrl("Saga")) shouldBe byteArrayOf(1)
        }

    @Test
    fun RNF26_namesWithDotsGoNowhereAndBombsAreRefused() =
        runTest {
            write("evil.cb7", sevenZ("../../evil.jpg" to byteArrayOf(6)))
            unpacker.names("evil.cb7") shouldContainExactly listOf("../../evil.jpg")
            unpacker.read("evil.cb7", "../../evil.jpg") shouldBe byteArrayOf(6)
            File(temp.parentFile, "evil.jpg").exists() shouldBe false
            val small = JvmArchiveUnpacker(tree, File(temp, "small"), maxEntryBytes = 100)
            write("big.cb7", sevenZ("big.jpg" to ByteArray(1000)))
            shouldThrow<SourceException> { small.names("big.cb7") }.kind shouldBe SourceException.Kind.UNAVAILABLE
            // A broken archive says so instead of crashing.
            write("broken.cbr", byteArrayOf(1, 2, 3))
            shouldThrow<SourceException> { unpacker.names("broken.cbr") }
        }

    @Test
    fun RF13_withoutTheLibrariesCbrSaysWhereItOpens() =
        runTest {
            write("a.cbr", fixture("rar4.rar"))
            val web = LocalSource(tree)
            val error =
                shouldThrow<SourceException> {
                    web.pages(SourceManga(SourceId.LOCAL, "a.cbr", "a"), web.chapters(SourceManga(SourceId.LOCAL, "a.cbr", "a")).single())
                }
            error.kind shouldBe SourceException.Kind.UNAVAILABLE
        }

    private val info =
        """
        <?xml version="1.0"?>
        <ComicInfo>
          <Title>O começo</Title>
          <Series>Saga</Series>
          <Number>1</Number>
          <Writer>Brian K. Vaughan</Writer>
          <Penciller>Fiona Staples</Penciller>
          <Summary>Alana &amp; Marko fogem com a filha.</Summary>
        </ComicInfo>
        """.trimIndent().encodeToByteArray()

    @Test
    fun RF14_comicInfoNamesTheChapterAndTheWork() =
        runTest {
            write("Saga/arquivo-sem-numero.cbz", cbz("ComicInfo.xml" to info, "01.jpg" to byteArrayOf(1)))
            write("Saga/Saga 02/01.jpg", byteArrayOf(2))
            write("Saga/Saga 02/ComicInfo.xml", "<ComicInfo><Series>Saga</Series><Number>2</Number></ComicInfo>".encodeToByteArray())
            val manga = SourceManga(SourceId.LOCAL, "Saga", "Saga")
            val chapters = source.chapters(manga)
            chapters.map { it.title } shouldContainExactly listOf("Saga #1", "Saga #2")
            chapters.map { it.number } shouldContainExactly listOf(1.0, 2.0)
            val details = source.details(manga)
            details.authors shouldContainExactly listOf("Brian K. Vaughan")
            details.artists shouldContainExactly listOf("Fiona Staples")
            details.description shouldBe "Alana & Marko fogem com a filha."
        }

    @Test
    fun RF14_withoutComicInfoTheNameAndFirstImageStay() =
        runTest {
            write("Inv/Ed 3.cbz", cbz("01.jpg" to byteArrayOf(1)))
            val manga = SourceManga(SourceId.LOCAL, "Inv", "Inv")
            source.chapters(manga).single().title shouldBe "Ed 3"
            source.details(manga).description shouldBe null
            ComicInfo.parse("<ComicInfo><Title>Só título &#233;</Title></ComicInfo>").label shouldBe "Só título é"
        }

    @Test
    fun RF15_readingAgainShowsNewAndRemovedFiles() =
        runTest {
            write("Saga/Saga 01.cbz", cbz("01.jpg" to byteArrayOf(1)))
            val manga = SourceManga(SourceId.LOCAL, "Saga", "Saga")
            source.chapters(manga).size shouldBe 1
            write("Saga/Saga 02.cbz", cbz("01.jpg" to byteArrayOf(2)))
            write("Kaiju.cbz", cbz("01.jpg" to byteArrayOf(3)))
            source.chapters(manga).map { it.number } shouldContainExactly listOf(1.0, 2.0)
            source.popular(1).items.map { it.title } shouldContainExactly listOf("Kaiju", "Saga")
            fs.delete(root / "Kaiju.cbz")
            source.popular(1).items.map { it.title } shouldContainExactly listOf("Saga")
        }
}
