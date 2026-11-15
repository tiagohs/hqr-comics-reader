package com.tiagohs.hqr.ui.sources

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tiagohs.hqr.data.net.ChallengeCookies
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled") // Cloudflare's check is a script.
@Composable
actual fun ChallengeBrowser(
    url: String,
    onPassed: () -> Unit,
    modifier: Modifier,
) {
    val passed by rememberUpdatedState(onPassed)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            CookieManager.getInstance().setAcceptCookie(true)
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        },
        onRelease = { it.destroy() },
    )
    // The clearance arrives as a cookie of the source's host: then the app's requests can carry it. One left from an
    // earlier verification may be expired on the server, so only a new value counts.
    // Kept across a rotation: read again after a new cookie arrived, it would wait for ever.
    val before =
        androidx.compose.runtime.saveable
            .rememberSaveable(url) { clearance(url).orEmpty() }
    LaunchedEffect(url) {
        while (true) {
            delay(POLL_MS)
            val now = clearance(url)
            if (now != null && now != before.ifEmpty { null }) {
                CookieManager.getInstance().flush()
                passed()
                break
            }
        }
    }
}

private const val POLL_MS = 1_000L

private fun clearance(url: String): String? =
    CookieManager
        .getInstance()
        .getCookie(url)
        ?.split(';')
        ?.map { it.trim() }
        ?.firstOrNull { it.startsWith(ChallengeCookies.CLEARANCE + "=") }
