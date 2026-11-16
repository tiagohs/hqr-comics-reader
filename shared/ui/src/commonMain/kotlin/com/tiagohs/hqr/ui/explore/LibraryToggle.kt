package com.tiagohs.hqr.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.LibraryRemoval
import com.tiagohs.hqr.ui.library.MarkReadingHost
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_undo
import com.tiagohs.hqr.ui.resources.browse_add_named
import com.tiagohs.hqr.ui.resources.browse_added_named
import com.tiagohs.hqr.ui.resources.browse_in_library_named
import com.tiagohs.hqr.ui.resources.library_removed
import com.tiagohs.hqr.ui.resources.mark_reading
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * RF-127: the button on a result's cover. Outside the library it puts the work in; inside, a filled ✓ that takes it
 * out (with "Desfazer"). 48 dp to touch, 36 dp to see.
 */
@Composable
fun BoxScope.LibraryToggleButton(
    title: String,
    inLibrary: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(if (inLibrary) Res.string.browse_in_library_named else Res.string.browse_add_named, title)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (inLibrary) MaterialTheme.colorScheme.primary else PAPER,
        contentColor = if (inLibrary) MaterialTheme.colorScheme.onPrimary else INK,
        border = BorderStroke(2.dp, if (inLibrary) PAPER else INK),
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(36.dp)
                .semantics {
                    contentDescription = label
                    role = Role.Button
                    stateDescription = ""
                },
    ) {
        Icon(
            if (inLibrary) HqrIcons.Check else HqrIcons.BookmarkAdd,
            contentDescription = null,
            modifier = Modifier.padding(7.dp),
        )
    }
}

/**
 * RF-52, RF-127: puts the work in the library or takes it out and says so. Put in, the message offers "Marcar leitura"
 * (RF-126); taken out, "Desfazer".
 */
@Suppress("LongParameterList") // The grid of a source and the global search pass their own ViewModel's three steps.
suspend fun toggleInLibrary(
    manga: SourceManga,
    snackbar: SnackbarHostState,
    marking: MarkReadingHost,
    toggle: suspend (SourceManga) -> LibraryRemoval?,
    undo: suspend (SourceManga, LibraryRemoval?) -> Unit,
    open: suspend (SourceManga) -> Long,
) {
    val removal = toggle(manga)
    if (removal == null) {
        val id = open(manga)
        val result =
            snackbar.showSnackbar(
                getString(Res.string.browse_added_named, manga.title),
                getString(Res.string.mark_reading),
                duration = SnackbarDuration.Long,
            )
        if (result == SnackbarResult.ActionPerformed) marking.open(id)
    } else {
        val result =
            snackbar.showSnackbar(
                getString(Res.string.library_removed),
                getString(Res.string.action_undo),
                duration = SnackbarDuration.Long,
            )
        if (result == SnackbarResult.ActionPerformed) undo(manga, removal)
    }
}

private val PAPER = Color(0xFFFFFDF7)
private val INK = Color(0xFF16161A)
