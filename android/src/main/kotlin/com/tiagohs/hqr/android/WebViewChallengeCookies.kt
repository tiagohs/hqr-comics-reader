package com.tiagohs.hqr.android

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import com.tiagohs.hqr.data.net.ChallengeCookies

/** RF-106: the cookies the app's browser kept after a Cloudflare verification, and its User-Agent. */
class WebViewChallengeCookies(
    private val context: Context,
) : ChallengeCookies {
    override fun cookies(url: String): String? =
        runCatching { CookieManager.getInstance().getCookie(url) }
            .getOrNull()
            ?.takeIf { ChallengeCookies.CLEARANCE in it }

    override val userAgent: String by lazy { WebSettings.getDefaultUserAgent(context) }
}
