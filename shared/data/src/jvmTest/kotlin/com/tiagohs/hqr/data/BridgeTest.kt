package com.tiagohs.hqr.data

import com.tiagohs.hqr.data.bridge.PersonalBridge
import com.tiagohs.hqr.data.bridge.routeThroughBridge
import com.tiagohs.hqr.data.net.hqrHttpClient
import com.tiagohs.hqr.data.net.hqrRetries
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.SourceException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/** The web app's way to the online sources: the Worker's bridge with the account's session (RF-108, ADR-027). */
class BridgeTest {
    private val seen = mutableListOf<HttpRequestData>()
    private var sourceStatus = HttpStatusCode.OK
    private var sourceBody = "ok"
    private val engine =
        MockEngine { request ->
            seen += request
            respond(sourceBody, sourceStatus)
        }
    private var session: String? = null
    private val signedIn = MutableStateFlow(false)
    private val bridge = PersonalBridge("https://hqr.tiago.workers.dev/bridge", signedIn) { session }
    private val client =
        HttpClient(engine) {
            hqrRetries(delayMs = 1)
            routeThroughBridge(bridge)
        }

    @Test
    fun RF108_signedOutNoRequestReachesTheSource() =
        runTest {
            shouldThrow<SourceException> { client.get("https://api.mangadex.org/manga") }.kind shouldBe SourceException.Kind.BRIDGE
            seen shouldBe emptyList()
            bridge.needs(SourceId.MANGADEX) shouldBe true
            bridge.needs(SourceId.LOCAL) shouldBe false
        }

    @Test
    fun RF108_RF109_sourceRequestsGoThroughTheBridgeWithTheSessionAndTheUserAgent() =
        runTest {
            session = "sessao"
            val target = "https://cmdxd98sb0x3yprd.mangadex.network/data/abc/1.png?x=a%20b"
            client
                .get(target) {
                    header(HttpHeaders.UserAgent, "HQR/1.0.0")
                    header(HttpHeaders.Referrer, "https://mangadex.org/")
                }.bodyAsText() shouldBe "ok"
            val sent = seen.single()
            sent.url.host shouldBe "hqr.tiago.workers.dev"
            sent.url.encodedPath shouldBe "/bridge/fetch"
            sent.url.parameters["url"] shouldBe target
            sent.headers[HttpHeaders.Authorization] shouldBe "Bearer sessao"
            sent.headers[PersonalBridge.UA_HEADER] shouldBe "HQR/1.0.0"
            sent.headers[PersonalBridge.REFERER_HEADER] shouldBe "https://mangadex.org/"
            sent.headers[HttpHeaders.Referrer] shouldBe null
        }

    @Test
    fun RF111_otherHostsAreNeverSentToTheBridge() =
        runTest {
            session = "sessao"
            client.get("https://komga.casa/opds/v1.2/catalog")
            seen.single().url.host shouldBe "komga.casa"
            seen.single().headers[HttpHeaders.Authorization] shouldBe null
        }

    @Test
    fun RF108_anEndedSessionAndAnOldWorkerAreBridgeProblems() =
        runTest {
            session = "antiga"
            sourceStatus = HttpStatusCode.Unauthorized
            sourceBody = """{"error":"session"}"""
            val ended = shouldThrow<SourceException> { client.get("https://api.mangadex.org/manga") }
            ended.kind shouldBe SourceException.Kind.BRIDGE
            ended.message shouldBe "Entre de novo para ler online"
            // The bridge's own refusal is final: asked once, never repeated.
            seen.size shouldBe 1
            sourceStatus = HttpStatusCode.Forbidden
            sourceBody = """{"error":"host"}"""
            val outdated = shouldThrow<SourceException> { client.get("https://weebcentral.com/search/data") }
            outdated.kind shouldBe SourceException.Kind.BRIDGE
            outdated.message shouldBe "O servidor do HQR ainda não abre esta fonte"
            // The shared client keeps working for everything else.
            hqrHttpClient("HQR") { routeThroughBridge(bridge) }.close()
        }

    @Test
    fun RF108_aRefusalFromTheSourceItselfIsNotABridgeProblem() =
        runTest {
            session = "sessao"
            sourceStatus = HttpStatusCode.Forbidden
            sourceBody = "<html>Attention Required</html>"
            val response = client.get("https://weebcentral.com/search/data")
            // It reaches the source's own reading (ensureOk: access refused), not "session ended".
            response.status shouldBe HttpStatusCode.Forbidden
            sourceBody = """{"error":"redirect"}"""
            shouldThrow<SourceException> { client.get("https://weebcentral.com/x") }.kind shouldBe SourceException.Kind.NOT_FOUND
        }
}
