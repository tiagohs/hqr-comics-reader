package com.tiagohs.hqr.presentation.reader

/** The keys the reader answers to (RF-112). */
enum class ReaderKey {
    LEFT,
    RIGHT,
    UP,
    DOWN,
    SPACE,
    SHIFT_SPACE,
    PAGE_UP,
    PAGE_DOWN,
    HOME,
    END,
    F,
    QUESTION,
    ESCAPE,
    VOLUME_UP,
    VOLUME_DOWN,

    /** A controller's right shoulder (RF-47). */
    NEXT_BUTTON,

    /** A controller's left shoulder (RF-47). */
    PREVIOUS_BUTTON,
}

enum class KeyAction { NEXT, PREVIOUS, FIRST, LAST, FULLSCREEN, SHORTCUTS, CLOSE }

/**
 * RF-112: keyboard reading. Space and Page Down always go forward; the arrows follow the page on screen, so in
 * right-to-left the left arrow is the next page, as in a printed manga.
 */
object ReaderKeys {
    fun action(
        key: ReaderKey,
        mode: ReaderMode,
        volumeKeys: Boolean = false,
    ): KeyAction? =
        when (key) {
            // RF-47: only when the option is on; otherwise the volume stays the volume.
            ReaderKey.VOLUME_DOWN -> KeyAction.NEXT.takeIf { volumeKeys }
            ReaderKey.VOLUME_UP -> KeyAction.PREVIOUS.takeIf { volumeKeys }
            ReaderKey.NEXT_BUTTON -> KeyAction.NEXT
            ReaderKey.PREVIOUS_BUTTON -> KeyAction.PREVIOUS
            ReaderKey.SPACE, ReaderKey.PAGE_DOWN, ReaderKey.DOWN -> KeyAction.NEXT
            ReaderKey.SHIFT_SPACE, ReaderKey.PAGE_UP, ReaderKey.UP -> KeyAction.PREVIOUS
            ReaderKey.RIGHT -> if (mode == ReaderMode.RIGHT_TO_LEFT) KeyAction.PREVIOUS else KeyAction.NEXT
            ReaderKey.LEFT -> if (mode == ReaderMode.RIGHT_TO_LEFT) KeyAction.NEXT else KeyAction.PREVIOUS
            ReaderKey.HOME -> KeyAction.FIRST
            ReaderKey.END -> KeyAction.LAST
            ReaderKey.F -> KeyAction.FULLSCREEN
            ReaderKey.QUESTION -> KeyAction.SHORTCUTS
            ReaderKey.ESCAPE -> KeyAction.CLOSE
        }
}
