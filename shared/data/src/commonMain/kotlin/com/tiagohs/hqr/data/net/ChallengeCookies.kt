package com.tiagohs.hqr.data.net

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders

/**
 * RF-106: what the browser inside the app kept after the user passed a Cloudflare verification — the cookies of a
 * host and the browser's User-Agent, which Cloudflare ties the clearance to. Null where there is no such browser.
 */
interface ChallengeCookies {
    /** The Cookie header for [url], only when it carries a clearance; null otherwise. */
    fun cookies(url: String): String?

    val userAgent: String

    companion object {
        const val CLEARANCE = "cf_clearance"
    }
}

/** RF-106: requests to a host with a clearance go with its cookies and the browser's User-Agent. */
fun HttpClientConfig<*>.useChallengeCookies(provider: ChallengeCookies) {
    install(
        createClientPlugin("ChallengeCookies") {
            onRequest { request, _ ->
                val cookies = provider.cookies(request.url.buildString()) ?: return@onRequest
                val existing = request.headers[HttpHeaders.Cookie]
                request.headers[HttpHeaders.Cookie] = listOfNotNull(existing, cookies).joinToString("; ")
                request.headers[HttpHeaders.UserAgent] = provider.userAgent
            }
        },
    )
}
