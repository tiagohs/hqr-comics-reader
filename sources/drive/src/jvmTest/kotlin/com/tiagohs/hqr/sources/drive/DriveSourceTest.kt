package com.tiagohs.hqr.sources.drive

import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.source.SourceException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DriveSourceTest {
    private val seen = mutableListOf<Pair<String, String?>>()

    private val rendered = mutableListOf<Triple<String, String, Int>>()
    private val fakePdf =
        object : PdfRenderer {
            override suspend fun pageCount(
                url: String,
                token: String,
            ) = 3

            override suspend fun render(
                url: String,
                token: String,
                page: Int,
            ): ByteArray {
                rendered += Triple(url, token, page)
                return byteArrayOf(9)
            }
        }

    private fun source(
        token: String? = "session-1",
        other: String = DETAILS,
        pdf: PdfRenderer? = fakePdf,
    ) = DriveSource(
        HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath
                seen += path to request.headers[HttpHeaders.Authorization]
                if (path.contains("/files/")) {
                    respond(byteArrayOf(1, 2, 3), HttpStatusCode.OK)
                } else {
                    respond(
                        content = if (path.endsWith("/works")) WORKS else other,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            },
        ),
        hubUrl = { "https://hqr.example" },
        session = { token },
        pdf = pdf,
    )

    @Test
    fun RF130_the_catalog_lists_the_works_with_the_shelf_of_their_folder() =
        runTest {
            val page = source().popular(1)
            page.items.map { it.title to it.kind } shouldBe
                listOf("Turma da Mônica Jovem" to ReadingKind.MANGA, "Turma da Mônica" to ReadingKind.COMIC)
            page.items.first().coverUrl shouldBe "drive://file/poster1"
            seen.first() shouldBe ("/api/v1/drive/works" to "Bearer session-1")
        }

    @Test
    fun RF130_the_search_matches_part_of_the_name_without_caring_for_case() =
        runTest {
            source().search("jovem", emptyList(), 1).items.map { it.key } shouldBe listOf("work1")
        }

    @Test
    fun RF130_the_list_is_asked_once_for_browsing_and_searching() =
        runTest {
            val drive = source()
            drive.popular(1)
            drive.search("mônica", emptyList(), 1)
            drive.latest(1)
            seen.count { it.first.endsWith("/works") } shouldBe 1
        }

    @Test
    fun RF130_details_and_chapters_come_from_the_info_and_the_folders() =
        runTest {
            val drive = source()
            val manga = drive.popular(1).items.first()
            val detailed = drive.details(manga)
            detailed.authors shouldBe listOf("Mauricio de Sousa")
            detailed.status shouldBe PublicationStatus.ONGOING
            detailed.description shouldBe "Os personagens da turma como adolescentes."
            val chapters = drive.chapters(detailed)
            chapters.map { it.number } shouldBe listOf(1.0, 2.0, 10.0)
            chapters.map { it.title } shouldBe listOf("O começo", null, null)
            chapters.first().key shouldBe "ch1"
        }

    @Test
    fun RF130_a_missing_or_broken_info_still_opens_the_work_with_the_folder_name() =
        runTest {
            val drive = source(other = """{"info":null,"chapters":[{"id":"c1","name":"Volume 3"}]}""")
            val manga = drive.popular(1).items.first()
            drive.details(manga).title shouldBe "Turma da Mônica Jovem"
            drive.chapters(manga).single().number shouldBe 3.0
            DriveInfo.parse("{not json").title shouldBe null
        }

    @Test
    fun RF130_a_chapter_folder_without_a_number_takes_its_place_in_the_order() {
        DriveSource.chapterNumber("Extra") shouldBe null
        DriveSource.chapterNumber("Capítulo 12,5") shouldBe 12.5
        DriveSource.chapterNumber("01") shouldBe 1.0
    }

    @Test
    fun RF130_pages_come_as_drive_files_in_the_order_the_worker_gave() =
        runTest {
            val drive = source(other = """{"pages":[{"id":"p1","name":"1.jpg"},{"id":"p2","name":"2.jpg"}]}""")
            val manga = drive.popular(1).items.first()
            drive.pages(manga, SourceChapter("ch1", 1.0)).map { it.imageUrl } shouldBe listOf("drive://file/p1", "drive://file/p2")
        }

    @Test
    fun RF131_a_pdf_chapter_has_one_page_per_pdf_page_rendered_on_the_device() =
        runTest {
            val drive = source(other = """{"info":null,"chapters":[{"id":"f1","name":"10","kind":"pdf"}]}""")
            val manga = drive.popular(1).items.first()
            val chapter = drive.chapters(manga).single()
            chapter.key shouldBe "pdf:f1"
            val pages = drive.pages(manga, chapter)
            pages.map { it.imageUrl } shouldBe listOf("drive://pdf/f1#0", "drive://pdf/f1#1", "drive://pdf/f1#2")
            drive.image(pages[2].imageUrl, emptyMap()) shouldBe byteArrayOf(9)
            rendered.single() shouldBe Triple("https://hqr.example/api/v1/drive/files/f1", "session-1", 2)
        }

    @Test
    fun RF131_a_folder_with_only_a_pdf_reads_as_that_pdf() =
        runTest {
            val drive = source(other = """{"pages":[],"pdf":"onlyPdf1"}""")
            val manga = drive.popular(1).items.first()
            drive.pages(manga, SourceChapter("folder1", 1.0)).map { it.imageUrl } shouldBe
                listOf("drive://pdf/onlyPdf1#0", "drive://pdf/onlyPdf1#1", "drive://pdf/onlyPdf1#2")
        }

    @Test
    fun RF131_where_the_platform_has_no_pdf_renderer_the_chapter_says_so() =
        runTest {
            val drive = source(pdf = null)
            val manga = drive.popular(1).items.first()
            shouldThrow<SourceException> { drive.pages(manga, SourceChapter("pdf:f1", 1.0)) }.kind shouldBe SourceException.Kind.UNAVAILABLE
        }

    @Test
    fun RF131_the_pdf_is_downloaded_once_for_all_its_pages() =
        runTest {
            var downloads = 0
            val engine =
                object : PdfEngine {
                    override suspend fun pageCount(
                        key: String,
                        bytes: ByteArray,
                    ) = bytes.size

                    override suspend fun render(
                        key: String,
                        bytes: ByteArray,
                        page: Int,
                    ) = byteArrayOf(page.toByte())
                }
            val client =
                HttpClient(
                    MockEngine {
                        downloads++
                        respond(byteArrayOf(1, 2, 3, 4), HttpStatusCode.OK)
                    },
                )
            val renderer = DownloadedPdfRenderer(client, engine)
            renderer.pageCount("https://hqr.example/f", "t") shouldBe 4
            renderer.render("https://hqr.example/f", "t", 1) shouldBe byteArrayOf(1)
            renderer.render("https://hqr.example/f", "t", 3) shouldBe byteArrayOf(3)
            downloads shouldBe 1
        }

    @Test
    fun RF130_an_image_is_fetched_with_the_session() =
        runTest {
            val bytes = source().image("drive://file/abc123", emptyMap())
            bytes.size shouldBe 3
            seen.last() shouldBe ("/api/v1/drive/files/abc123" to "Bearer session-1")
        }

    @Test
    fun RF130_signed_out_the_drive_asks_to_sign_in_without_calling_the_worker() =
        runTest {
            val drive = source(token = null)
            shouldThrow<SourceException> { drive.popular(1) }.kind shouldBe SourceException.Kind.AUTH
            shouldThrow<SourceException> { drive.image("drive://file/x", emptyMap()) }.kind shouldBe SourceException.Kind.AUTH
            seen shouldBe emptyList()
        }

    @Test
    fun RF130_the_status_words_in_portuguese_are_understood() {
        DriveInfo(status = "Completo").publicationStatus() shouldBe PublicationStatus.COMPLETED
        DriveInfo(status = "pausado").publicationStatus() shouldBe PublicationStatus.HIATUS
        DriveInfo(status = "???").publicationStatus() shouldBe PublicationStatus.UNKNOWN
    }

    private companion object {
        const val WORKS =
            """{"works":[
                {"id":"work1","shelf":"mangas","name":"Turma da Mônica Jovem","posterId":"poster1","modifiedTime":"2026-11-10T10:00:00.000Z"},
                {"id":"work2","shelf":"hqs","name":"Turma da Mônica","posterId":null,"modifiedTime":"2026-11-12T10:00:00.000Z"}]}"""
        const val DETAILS =
            """{"info":{"title":"Turma da Mônica Jovem","authors":["Mauricio de Sousa"],"status":"ongoing",
                "description":"Os personagens da turma como adolescentes.","chapters":{"01":"O começo"}},
                "chapters":[{"id":"ch1","name":"01"},{"id":"ch2","name":"02"},{"id":"ch10","name":"10"}]}"""
    }
}
