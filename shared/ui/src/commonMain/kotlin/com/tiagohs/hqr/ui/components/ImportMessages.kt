package com.tiagohs.hqr.ui.components

import androidx.compose.runtime.Composable
import com.tiagohs.hqr.sources.local.ImportResult
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.files_cbr_refused
import com.tiagohs.hqr.ui.resources.files_failed
import com.tiagohs.hqr.ui.resources.files_ignored
import com.tiagohs.hqr.ui.resources.files_imported
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** RF-110: the parts of an import that have something to say, in order; empty when nothing happened. */
fun importParts(result: ImportResult?): List<Pair<StringResource, List<String>>> =
    if (result == null) {
        emptyList()
    } else {
        listOf(
            Res.string.files_imported to result.imported,
            Res.string.files_cbr_refused to result.refused,
            Res.string.files_ignored to result.ignored,
            Res.string.files_failed to result.failed,
        ).filter { it.second.isNotEmpty() }
    }

/** True when any file didn't come in. */
fun ImportResult.hasProblems(): Boolean = refused.isNotEmpty() || ignored.isNotEmpty() || failed.isNotEmpty()

/** RF-110: what an import did, one line per kind (CBR and CB7 come with the reason). Null when nothing happened. */
@Composable
fun importMessage(result: ImportResult?): String? =
    importParts(result)
        .map { (text, names) -> stringResource(text, names.joinToString()) }
        .takeIf { it.isNotEmpty() }
        ?.joinToString("\n")
