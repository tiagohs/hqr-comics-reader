package com.tiagohs.hqr.ui

import com.tiagohs.hqr.domain.source.ServerProblem
import com.tiagohs.hqr.sources.local.ImportResult
import com.tiagohs.hqr.ui.components.hasProblems
import com.tiagohs.hqr.ui.components.importParts
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.files_cbr_refused
import com.tiagohs.hqr.ui.resources.files_failed
import com.tiagohs.hqr.ui.resources.files_ignored
import com.tiagohs.hqr.ui.resources.files_imported
import com.tiagohs.hqr.ui.resources.server_problem_cors
import com.tiagohs.hqr.ui.resources.server_problem_mixed
import com.tiagohs.hqr.ui.resources.server_problem_unreachable
import com.tiagohs.hqr.ui.resources.server_problem_wrong_login
import com.tiagohs.hqr.ui.sources.serverProblemText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What the web app says about imports and servers (RF-110, RF-111). */
class WebMessagesTest {
    @Test
    fun RF110_importSaysWhatCameInAndWhyTheRestDidnt() {
        val result = ImportResult(listOf("a.cbz"), listOf("b.cbr", "c.cb7"), listOf("d.jpg"), listOf("e.pdf"))
        assertEquals(
            listOf(
                Res.string.files_imported to listOf("a.cbz"),
                Res.string.files_cbr_refused to listOf("b.cbr", "c.cb7"),
                Res.string.files_ignored to listOf("d.jpg"),
                Res.string.files_failed to listOf("e.pdf"),
            ),
            importParts(result),
        )
        assertTrue(result.hasProblems())
        assertFalse(ImportResult(listOf("a.cbz"), emptyList()).hasProblems())
        assertEquals(emptyList(), importParts(ImportResult(emptyList(), emptyList())))
    }

    @Test
    fun RF111_unreachableServerInTheBrowserSaysWhatToAllow() {
        val origin = "https://tiagohs.github.io"
        assertEquals(
            Res.string.server_problem_cors to listOf(origin),
            serverProblemText(ServerProblem.UNREACHABLE, "https://komga.casa/opds", origin),
        )
        // An https page can't reach a plain http server at all: allowing the origin wouldn't help.
        assertEquals(
            Res.string.server_problem_mixed to emptyList(),
            serverProblemText(ServerProblem.UNREACHABLE, "http://192.168.0.10:25600/opds", origin),
        )
        assertEquals(
            Res.string.server_problem_cors to listOf(origin),
            serverProblemText(ServerProblem.UNREACHABLE, "http://localhost:25600/opds", origin),
        )
        // Outside the browser, and for other problems, the usual message.
        assertEquals(
            Res.string.server_problem_unreachable to emptyList(),
            serverProblemText(ServerProblem.UNREACHABLE, "https://komga.casa", null),
        )
        assertEquals(
            Res.string.server_problem_wrong_login to emptyList(),
            serverProblemText(ServerProblem.WRONG_LOGIN, "https://komga.casa", origin),
        )
    }
}
