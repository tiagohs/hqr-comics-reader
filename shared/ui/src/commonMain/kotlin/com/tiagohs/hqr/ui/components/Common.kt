package com.tiagohs.hqr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.error_auth
import com.tiagohs.hqr.ui.resources.error_bridge
import com.tiagohs.hqr.ui.resources.error_certificate
import com.tiagohs.hqr.ui.resources.error_challenge
import com.tiagohs.hqr.ui.resources.error_network
import com.tiagohs.hqr.ui.resources.error_not_found
import com.tiagohs.hqr.ui.resources.error_offline
import com.tiagohs.hqr.ui.resources.error_parse
import com.tiagohs.hqr.ui.resources.error_server
import com.tiagohs.hqr.ui.resources.error_unavailable
import com.tiagohs.hqr.ui.resources.error_unknown
import com.tiagohs.hqr.ui.resources.source_local
import com.tiagohs.hqr.ui.resources.status_cancelled
import com.tiagohs.hqr.ui.resources.status_completed
import com.tiagohs.hqr.ui.resources.status_hiatus
import com.tiagohs.hqr.ui.resources.status_ongoing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Reading progress on a halftone track (design.md §4). [fraction] 0..1; in right-to-left it fills from the right. */
@Composable
fun HalftoneProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(track)
            .halftone(MaterialTheme.colorScheme.outline, step = 5f, radius = 1f)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

fun PublicationStatus.label(): StringResource? =
    when (this) {
        PublicationStatus.ONGOING -> Res.string.status_ongoing
        PublicationStatus.COMPLETED -> Res.string.status_completed
        PublicationStatus.CANCELLED -> Res.string.status_cancelled
        PublicationStatus.HIATUS -> Res.string.status_hiatus
        PublicationStatus.UNKNOWN -> null
    }

fun ErrorKind.message(): StringResource =
    when (this) {
        ErrorKind.OFFLINE -> Res.string.error_offline
        ErrorKind.NETWORK -> Res.string.error_network
        ErrorKind.SERVER -> Res.string.error_server
        ErrorKind.PARSE -> Res.string.error_parse
        ErrorKind.NOT_FOUND -> Res.string.error_not_found
        ErrorKind.AUTH -> Res.string.error_auth
        ErrorKind.CHALLENGE -> Res.string.error_challenge
        ErrorKind.UNAVAILABLE -> Res.string.error_unavailable
        ErrorKind.CERTIFICATE -> Res.string.error_certificate
        ErrorKind.BRIDGE -> Res.string.error_bridge
        ErrorKind.UNKNOWN -> Res.string.error_unknown
    }

/** The source's name in the app language: built-in names that are words (local files) are translated (RNF-14). */
@Composable
fun sourceDisplayName(
    id: com.tiagohs.hqr.domain.model.SourceId,
    name: String,
): String = if (id == com.tiagohs.hqr.domain.model.SourceId.LOCAL) stringResource(Res.string.source_local) else name
