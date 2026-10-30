package com.tiagohs.hqr.ui.components

import androidx.compose.runtime.Composable
import com.tiagohs.hqr.sources.local.ImportResult
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.files_cbr_refused
import com.tiagohs.hqr.ui.resources.files_imported
import org.jetbrains.compose.resources.stringResource

/** RF-110: what an import did, in one line; CBR comes with its reason. Null when nothing happened. */
@Composable
fun importMessage(result: ImportResult?): String? =
    when {
        result == null -> null
        result.refused.isNotEmpty() -> stringResource(Res.string.files_cbr_refused, result.refused.joinToString())
        result.imported.isNotEmpty() -> stringResource(Res.string.files_imported, result.imported.joinToString())
        else -> null
    }
