package com.tiagohs.hqr.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The icon set of the canvas, drawn on a 24 grid with a 1.8 stroke (design.md §5). Kept as SVG paths so the same
 * drawings serve Android and the web without an icon library.
 */
object HqrIcons {
    private fun stroke(
        name: String,
        vararg paths: String,
        width: Float = 1.8f,
    ): ImageVector =
        ImageVector
            .Builder(name, 24.dp, 24.dp, 24f, 24f)
            .apply {
                paths.forEach {
                    addPath(
                        pathData = addPathNodes(it),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = width,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }.build()

    private fun fill(
        name: String,
        vararg paths: String,
    ): ImageVector =
        ImageVector
            .Builder(name, 24.dp, 24.dp, 24f, 24f)
            .apply { paths.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) } }
            .build()

    private const val CIRCLE = "a8.5 8.5 0 1 0 0 17a8.5 8.5 0 1 0 0-17z"

    val Library = stroke("library", "M4 4h5v16H4zM10 4h5v16h-5zM16.5 5.2l3.4-.8 2.6 15-3.4.8z")
    val Explore = stroke("explore", "M12 3.5$CIRCLE", "M15.5 8.5l-2 5-5 2 2-5z")
    val History = stroke("history", "M12 3.5$CIRCLE", "M12 7.5V12l3 2")
    val Settings =
        stroke(
            "settings",
            "M12 8.5a3.5 3.5 0 1 0 0 7a3.5 3.5 0 1 0 0-7z",
            "M19.4 13a7.5 7.5 0 0 0 0-2l2-1.6-2-3.4-2.4 1a7.6 7.6 0 0 0-1.7-1L15 3.5h-4l-.4 2.5a7.6 7.6 0 0 " +
                "0-1.7 1l-2.4-1-2 3.4 2 1.6a7.5 7.5 0 0 0 0 2l-2 1.6 2 3.4 2.4-1a7.6 7.6 0 0 0 1.7 1l.4 " +
                "2.5h4l.4-2.5a7.6 7.6 0 0 0 1.7-1l2.4 1 2-3.4z",
        )
    val Search = stroke("search", "M11 4.5a6.5 6.5 0 1 0 0 13a6.5 6.5 0 1 0 0-13z", "M16 16l4.5 4.5")
    val Download = stroke("download", "M12 4v11M7 10l5 5 5-5M5 20h14")
    val Downloaded = stroke("downloaded", "M4 12.5l4.5 4.5L20 6M5 21h14")
    val Back = stroke("back", "M19 12H5M11 6l-6 6 6 6")
    val Close = stroke("close", "M6 6l12 12M18 6L6 18")
    val Tune = stroke("tune", "M4 7h10M18 7h2M4 17h4M12 17h8M16 5v4M10 15v4")
    val Filter = stroke("filter", "M4 6h16M7 12h10M10 18h4")
    val Sort = stroke("sort", "M7 4v16M3 16l4 4 4-4M17 20V4M13 8l4-4 4 4")
    val Check = stroke("check", "M5 12.5l4.5 4.5L19 7.5", width = 2.4f)
    val Globe = stroke("globe", "M12 3.5$CIRCLE", "M3.5 12h17M12 3.5c2.5 2.6 2.5 14.4 0 17M12 3.5c-2.5 2.6-2.5 14.4 0 17")
    val Folder = stroke("folder", "M3 6h7l2 2h9v11H3z")
    val Error = stroke("error", "M12 3.5$CIRCLE", "M12 7.5v5.5M12 16v.5")
    val Warning = stroke("warning", "M12 3.5L21 19.5H3z", "M12 10v4M12 17v.5")
    val Refresh = stroke("refresh", "M20 12a8 8 0 1 1-2.3-5.7M20 4v5h-5")
    val Offline = stroke("offline", "M2 9a15 15 0 0 1 20 0M5.5 12.5a10 10 0 0 1 13 0M9 16a5 5 0 0 1 6 0M3 3l18 18")
    val Pause = fill("pause", "M6 5h4v14H6zM14 5h4v14h-4z")
    val Play = fill("play", "M7 4.5v15l12-7.5z")
    val Bookmark = fill("bookmark", "M6 3h12v18l-6-4-6 4z")
    val BookmarkOutline = stroke("bookmarkOutline", "M6 3h12v18l-6-4-6 4z")
    val More =
        fill(
            "more",
            "M12 3.3a1.7 1.7 0 1 0 0 3.4a1.7 1.7 0 1 0 0-3.4zM12 10.3a1.7 1.7 0 1 0 0 3.4a1.7 1.7 0 1 0 " +
                "0-3.4zM12 17.3a1.7 1.7 0 1 0 0 3.4a1.7 1.7 0 1 0 0-3.4z",
        )
    val PrevChapter = stroke("prevChapter", "M6 5v14M19 5l-9 7 9 7z")
    val NextChapter = stroke("nextChapter", "M18 5v14M5 5l9 7-9 7z")
    val Delete = stroke("delete", "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13")
    val Add = stroke("add", "M12 5v14M5 12h14")
    val ArrowUp = stroke("arrowUp", "M12 19V5M6 11l6-6 6 6")
    val ArrowDown = stroke("arrowDown", "M12 5v14M6 13l6 6 6-6")
    val EditNote = stroke("editNote", "M4 6h12M4 11h9M4 16h6M14.5 19.5l6-6-2-2-6 6-.5 2.5z")
    val Drag = stroke("drag", "M8 6h.01M8 12h.01M8 18h.01M16 6h.01M16 12h.01M16 18h.01", width = 3f)
    val Panel = stroke("panel", "M4 4h16v16H4zM4 12h16M12 4v8")
    val Info = stroke("info", "M12 3.5$CIRCLE", "M12 11v5.5M12 7.5v.5")
    val Server = stroke("server", "M4 5h16v6H4zM4 13h16v6H4zM8 8h.01M8 16h.01")
    val Expand = stroke("expand", "M6 9l6 6 6-6")
}
