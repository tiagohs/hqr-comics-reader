package com.tiagohs.hqr.ui.reader

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import com.tiagohs.hqr.presentation.reader.KeyAction
import com.tiagohs.hqr.presentation.reader.ReaderKey
import com.tiagohs.hqr.presentation.reader.ReaderKeys
import com.tiagohs.hqr.presentation.reader.ReaderMode
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_ok
import com.tiagohs.hqr.ui.resources.reader_shortcut_ends
import com.tiagohs.hqr.ui.resources.reader_shortcut_fullscreen
import com.tiagohs.hqr.ui.resources.reader_shortcut_help
import com.tiagohs.hqr.ui.resources.reader_shortcut_pages
import com.tiagohs.hqr.ui.resources.reader_shortcut_wheel
import com.tiagohs.hqr.ui.resources.reader_shortcuts
import com.tiagohs.hqr.ui.settings.runsInBrowser
import org.jetbrains.compose.resources.stringResource

private const val MOUSE_IDLE_MS = 2_000L
private const val QUESTION_MARK = '?'.code

private fun KeyEvent.toReaderKey(): ReaderKey? =
    when (key) {
        Key.DirectionLeft -> ReaderKey.LEFT
        Key.DirectionRight -> ReaderKey.RIGHT
        Key.DirectionUp -> ReaderKey.UP
        Key.DirectionDown -> ReaderKey.DOWN
        Key.Spacebar -> if (isShiftPressed) ReaderKey.SHIFT_SPACE else ReaderKey.SPACE
        Key.PageUp -> ReaderKey.PAGE_UP
        Key.PageDown -> ReaderKey.PAGE_DOWN
        Key.MoveHome -> ReaderKey.HOME
        Key.MoveEnd -> ReaderKey.END
        Key.F -> ReaderKey.F
        Key.Escape -> ReaderKey.ESCAPE
        // "?" is Shift + "/" on most layouts; the typed character covers the others.
        Key.Slash -> if (isShiftPressed) ReaderKey.QUESTION else null
        else -> if (utf16CodePoint == QUESTION_MARK) ReaderKey.QUESTION else null
    }

/**
 * RF-112: the reader by keyboard and mouse. Keys become [KeyAction]s; moving the mouse shows the controls and 2 s
 * without moving hides them. Touch is left to the pages.
 */
@Composable
fun Modifier.readerInput(
    mode: ReaderMode,
    menuVisible: Boolean,
    /** True while a dialog or sheet is open: when it closes, the keys come back to the reader. */
    overlayOpen: Boolean,
    onAction: (KeyAction) -> Unit,
    onMouseMoved: () -> Unit,
    onMouseIdle: () -> Unit,
): Modifier {
    val focus = remember { FocusRequester() }
    LaunchedEffect(overlayOpen, menuVisible) { if (!overlayOpen) runCatching { focus.requestFocus() } }
    // Each move restarts the wait.
    val moves = remember { kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED) }
    LaunchedEffect(Unit) {
        while (true) {
            moves.receive()
            onMouseMoved()
            while (kotlinx.coroutines.withTimeoutOrNull(MOUSE_IDLE_MS) { moves.receive() } != null) Unit
            onMouseIdle()
        }
    }
    return this
        .focusRequester(focus)
        .focusable()
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val action = event.toReaderKey()?.let { ReaderKeys.action(it, mode) } ?: return@onKeyEvent false
            // Escape with the controls open just closes them.
            if (action == KeyAction.CLOSE && menuVisible) onMouseIdle() else onAction(action)
            true
        }.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    // Only in the browser (RF-112): on a phone with a mouse, taps keep their meaning.
                    val hover = event.type == PointerEventType.Move && event.changes.any { it.type == PointerType.Mouse && !it.pressed }
                    if (runsInBrowser && hover) moves.trySend(Unit)
                }
            }
        }
}

/** RF-112: the list "?" opens. */
@Composable
fun ShortcutsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.reader_shortcuts)) },
        text = {
            Column {
                listOf(
                    Res.string.reader_shortcut_pages,
                    Res.string.reader_shortcut_ends,
                    Res.string.reader_shortcut_wheel,
                    Res.string.reader_shortcut_fullscreen,
                    Res.string.reader_shortcut_help,
                ).forEach { Text(stringResource(it)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_ok)) } },
    )
}
