package com.tiagohs.hqr.data

import com.tiagohs.hqr.data.net.ChallengeCookies
import com.tiagohs.hqr.data.net.useChallengeCookies
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/** RF-106: after a verification, requests to that host carry its clearance and the browser's User-Agent. */
class ChallengeCookiesTest {
    @Test
    fun RF106_clearanceCookiesAndUserAgentOnlyWhereThereIsOne() =
        runTest {
            val seen = mutableListOf<HttpRequestData>()
            val provider =
                object : ChallengeCookies {
                    override fun cookies(url: String) = if ("weebcentral.com" in url) "cf_clearance=abc; theme=dark" else null

                    override val userAgent = "Mozilla/5.0 (Linux; Android 15) WebView"
                }
            val client =
                HttpClient(
                    MockEngine { request ->
                        seen += request
                        respond("ok")
                    },
                ) {
                    defaultRequest { header(HttpHeaders.UserAgent, "HQR/1.0") }
                    useChallengeCookies(provider)
                }
            client.get("https://weebcentral.com/search/data")
            client.get("https://api.mangadex.org/manga")
            seen[0].headers[HttpHeaders.Cookie] shouldBe "cf_clearance=abc; theme=dark"
            seen[0].headers[HttpHeaders.UserAgent] shouldBe "Mozilla/5.0 (Linux; Android 15) WebView"
            seen[1].headers[HttpHeaders.Cookie] shouldBe null
            seen[1].headers[HttpHeaders.UserAgent] shouldBe "HQR/1.0"
        }
}
