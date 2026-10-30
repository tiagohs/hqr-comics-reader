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
    // The clearance arrives as a cookie of the source's host: then the app's requests can carry it.
    LaunchedEffect(url) {
        while (true) {
            delay(POLL_MS)
            if (CookieManager.getInstance().getCookie(url)?.contains(ChallengeCookies.CLEARANCE) == true) {
                CookieManager.getInstance().flush()
                passed()
                break
            }
        }
    }
}

private const val POLL_MS = 1_000L
