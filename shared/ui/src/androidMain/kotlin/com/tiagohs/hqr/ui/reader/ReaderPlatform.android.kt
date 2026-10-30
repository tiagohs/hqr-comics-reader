package com.tiagohs.hqr.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.Bitmap
import com.tiagohs.hqr.presentation.reader.BorderCrop

@Composable
actual fun KeepScreenOn(on: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, on) {
        view.keepScreenOn = on
        onDispose { view.keepScreenOn = false }
    }
}

private tailrec fun Context.activity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }

@Composable
actual fun ImmersiveMode(hideBars: Boolean) {
    val window = LocalContext.current.activity()?.window ?: return
    val view = LocalView.current
    DisposableEffect(window, hideBars) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hideBars) controller.hide(WindowInsetsCompat.Type.systemBars()) else controller.show(WindowInsetsCompat.Type.systemBars())
        val before = window.attributes.layoutInDisplayCutoutMode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes =
                window.attributes.apply { layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {
                window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = before }
            }
        }
    }
}

internal actual fun Bitmap.widthPx(): Int = width

internal actual fun Bitmap.heightPx(): Int = height

internal actual fun Bitmap.argbAt(
    x: Int,
    y: Int,
): Int = getPixel(x, y)

internal actual fun Bitmap.cropTo(area: BorderCrop.Area): Bitmap = Bitmap.createBitmap(this, area.left, area.top, area.width, area.height)
