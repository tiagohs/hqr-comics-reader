package com.tiagohs.hqr.data

import com.tiagohs.hqr.data.net.ChallengeCookies
import com.tiagohs.hqr.data.net.ensureOk
import com.tiagohs.hqr.data.net.useChallengeCookies
import com.tiagohs.hqr.domain.source.SourceException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
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

    @Test
    fun RF106_onlyTheMarkedChallengeIsAVerification() =
        runTest {
            val http =
                HttpClient(
                    MockEngine { request ->
                        when (request.url.encodedPath) {
                            "/challenge" -> respond("", HttpStatusCode.ServiceUnavailable, headersOf("cf-mitigated", "challenge"))
                            "/waf" -> respond("Attention Required", HttpStatusCode.Forbidden, headersOf(HttpHeaders.Server, "cloudflare"))
                            else -> respond("down", HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.Server, "cloudflare"))
                        }
                    },
                )

            suspend fun kind(path: String) = shouldThrow<SourceException> { http.get("https://x.example$path").ensureOk() }.kind
            kind("/challenge") shouldBe SourceException.Kind.CHALLENGE
            // A WAF block and an origin that is down are what they are, not a verification the user can pass.
            kind("/waf") shouldBe SourceException.Kind.AUTH
            kind("/down") shouldBe SourceException.Kind.SERVER
        }
}
