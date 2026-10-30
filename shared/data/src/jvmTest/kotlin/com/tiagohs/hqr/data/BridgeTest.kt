package com.tiagohs.hqr.data

import com.russhwolf.settings.MapSettings
import com.tiagohs.hqr.data.bridge.PersonalBridge
import com.tiagohs.hqr.data.bridge.routeThroughBridge
import com.tiagohs.hqr.data.net.hqrHttpClient
import com.tiagohs.hqr.data.settings.SettingsSecretStore
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.BridgeCheck
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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/** The web app's way to the online sources (RF-108, ADR-023). */
class BridgeTest {
    private val seen = mutableListOf<HttpRequestData>()
    private var healthStatus = HttpStatusCode.OK
    private var healthBody = """{"ok":true,"version":2}"""
    private var sourceStatus = HttpStatusCode.OK
    private var sourceBody = "ok"
    private val engine =
        MockEngine { request ->
            seen += request
            if (request.url.encodedPath.endsWith("/health")) {
                respond(healthBody, healthStatus)
            } else {
                respond(sourceBody, sourceStatus)
            }
        }
    private val settings = MapSettings()
    private val bridge = PersonalBridge(settings, SettingsSecretStore(settings), HttpClient(engine))
    private val client = HttpClient(engine) { routeThroughBridge(bridge) }

    @Test
    fun RF108_withoutABridgeNoRequestReachesTheSource() =
        runTest {
            shouldThrow<SourceException> { client.get("https://api.mangadex.org/manga") }.kind shouldBe SourceException.Kind.BRIDGE
            seen shouldBe emptyList()
            bridge.needs(SourceId.MANGADEX) shouldBe true
            bridge.needs(SourceId.LOCAL) shouldBe false
        }

    @Test
    fun RF108_RF109_sourceRequestsGoThroughTheBridgeWithTheTokenAndTheUserAgent() =
        runTest {
            bridge.save("https://hqr.tiago.workers.dev/", " segredo ")
            val target = "https://cmdxd98sb0x3yprd.mangadex.network/data/abc/1.png?x=a%20b"
            client
                .get(target) {
                    header(HttpHeaders.UserAgent, "HQR/1.0.0")
                    header(HttpHeaders.Referrer, "https://mangadex.org/")
                }.bodyAsText() shouldBe "ok"
            val sent = seen.single()
            sent.url.host shouldBe "hqr.tiago.workers.dev"
            sent.url.encodedPath shouldBe "/fetch"
            sent.url.parameters["url"] shouldBe target
            sent.headers[PersonalBridge.TOKEN_HEADER] shouldBe "segredo"
            sent.headers[PersonalBridge.UA_HEADER] shouldBe "HQR/1.0.0"
            sent.headers[PersonalBridge.REFERER_HEADER] shouldBe "https://mangadex.org/"
            sent.headers[HttpHeaders.Referrer] shouldBe null
        }

    @Test
    fun RF111_otherHostsAreNeverSentToTheBridge() =
        runTest {
            bridge.save("https://hqr.tiago.workers.dev", "segredo")
            client.get("https://komga.casa/opds/v1.2/catalog")
            seen.single().url.host shouldBe "komga.casa"
            seen.single().headers[PersonalBridge.TOKEN_HEADER] shouldBe null
        }

    @Test
    fun RF108_testSaysWhatIsWrong() =
        runTest {
            bridge.test("http://hqr.workers.dev", "x") shouldBe BridgeCheck.INVALID_ADDRESS
            bridge.test("https://user:pw@hqr.workers.dev", "x") shouldBe BridgeCheck.INVALID_ADDRESS
            bridge.test("https://hqr.workers.dev", "certo") shouldBe BridgeCheck.OK
            seen.last().headers[PersonalBridge.TOKEN_HEADER] shouldBe "certo"
            healthStatus = HttpStatusCode.Forbidden
            bridge.test("https://hqr.workers.dev", "errado") shouldBe BridgeCheck.TOKEN_REFUSED
            healthStatus = HttpStatusCode.BadGateway
            bridge.test("https://hqr.workers.dev", "certo") shouldBe BridgeCheck.UNREACHABLE
            // A site that answers 200 without being a bridge is not saved as one.
            healthBody = "<html>ok</html>"
            healthStatus = HttpStatusCode.OK
            bridge.test("https://example.com", "certo") shouldBe BridgeCheck.UNREACHABLE
            // A bridge published before the sources of 1.1 works, but says it needs publishing again.
            healthBody = """{"ok":true,"version":1}"""
            bridge.test("https://hqr.workers.dev", "certo") shouldBe BridgeCheck.OUTDATED
        }

    @Test
    fun RF108_aTokenRefusedLaterIsABridgeProblem() =
        runTest {
            bridge.save("https://hqr.tiago.workers.dev", "antigo")
            sourceStatus = HttpStatusCode.Forbidden
            shouldThrow<SourceException> { client.get("https://api.mangadex.org/manga") }.kind shouldBe SourceException.Kind.BRIDGE
            // A bridge published before a source refuses its host: it asks to be published again.
            sourceBody = """{"error":"host"}"""
            val outdated = shouldThrow<SourceException> { client.get("https://weebcentral.com/search/data") }
            outdated.kind shouldBe SourceException.Kind.BRIDGE
            outdated.message shouldBe "Ponte desatualizada: publique de novo"
        }

    @Test
    fun RF108_clearForgetsAddressAndToken() =
        runTest {
            bridge.save("https://hqr.workers.dev", "segredo")
            bridge.clear()
            bridge.address.value shouldBe null
            bridge.current() shouldBe null
            // The shared client keeps working for everything else.
            hqrHttpClient("HQR") { routeThroughBridge(bridge) }.close()
        }
}
