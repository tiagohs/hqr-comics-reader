package com.tiagohs.hqr.data.bridge

import com.russhwolf.settings.Settings
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.Bridge
import com.tiagohs.hqr.domain.source.BridgeCheck
import com.tiagohs.hqr.domain.source.SecretStore
import com.tiagohs.hqr.domain.source.SourceException
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The user's bridge (RF-108, ADR-023): address in the settings, token in the secret store. The client of the web app
 * sends every request for a source host through it (see [routeThroughBridge]).
 */
class PersonalBridge(
    private val settings: Settings,
    private val secrets: SecretStore,
    private val http: HttpClient,
) : Bridge {
    private val _address = MutableStateFlow(settings.getStringOrNull(KEY_ADDRESS))
    override val address: StateFlow<String?> = _address.asStateFlow()

    /** Read once per request by the client plugin; kept in memory after the first read. */
    private var token: String? = null

    override fun needs(id: SourceId): Boolean = id in ONLINE

    override suspend fun test(
        address: String,
        token: String,
    ): BridgeCheck {
        val base = normalize(address) ?: return BridgeCheck.INVALID_ADDRESS
        return try {
            val response = http.get("$base/health") { header(TOKEN_HEADER, token.trim()) }
            when (response.status.value) {
                // Only the bridge answers {"ok":true}: any other site at that address is not one.
                OK -> health(response.bodyAsText())
                FORBIDDEN, UNAUTHORIZED -> BridgeCheck.TOKEN_REFUSED
                else -> BridgeCheck.UNREACHABLE
            }
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
        ) {
            BridgeCheck.UNREACHABLE
        }
    }

    private fun health(body: String): BridgeCheck {
        if (!body.contains("\"ok\":true")) return BridgeCheck.UNREACHABLE
        val version =
            VERSION
                .find(body)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull() ?: 0
        return if (version < VERSION_NEEDED) BridgeCheck.OUTDATED else BridgeCheck.OK
    }

    override suspend fun save(
        address: String,
        token: String,
    ) {
        val base = normalize(address) ?: return
        secrets.put(KEY_TOKEN, token.trim())
        this.token = token.trim()
        settings.putString(KEY_ADDRESS, base)
        _address.value = base
    }

    override suspend fun clear() {
        secrets.remove(KEY_TOKEN)
        token = null
        settings.remove(KEY_ADDRESS)
        _address.value = null
    }

    /** Address and token for a request, or null without a bridge. */
    suspend fun current(): Pair<String, String>? {
        val base = _address.value ?: return null
        val secret = token ?: secrets.get(KEY_TOKEN)?.also { token = it } ?: return null
        return base to secret
    }

    companion object {
        const val TOKEN_HEADER = "X-HQR-Token"
        const val UA_HEADER = "X-HQR-UA"
        const val REFERER_HEADER = "X-HQR-Referer"
        private const val KEY_ADDRESS = "bridge.address"
        private const val KEY_TOKEN = "bridge.token"
        private const val OK = 200
        private const val UNAUTHORIZED = 401

        /** The bridge version that has every source's hosts (bridge-worker/ BRIDGE_VERSION). */
        const val VERSION_NEEDED = 2
        private val VERSION = Regex("\"version\"\\s*:\\s*(\\d+)")

        /** Sources whose hosts send no CORS (RF-108); local files and the user's servers never need the bridge. */
        val ONLINE =
            setOf(
                SourceId.MANGADEX,
                SourceId.HQ_NOW,
                SourceId("namicomi"),
                SourceId("archive"),
                SourceId("yomu"),
                SourceId("weebcentral"),
                SourceId("xoxo"),
            )

        /**
         * The hosts behind [ONLINE]; the same list as the bridge's default `HOSTS` (bridge-worker/). MangaFlix and the
         * Internet Archive's page images answer the browser themselves (CORS): they never go through the bridge.
         */
        private val HOSTS =
            setOf(
                "api.mangadex.org",
                "uploads.mangadex.org",
                "api.mangadex.network",
                "admin.hq-now.com",
                "www.hq-now.com",
                "archive.org",
                "api.namicomi.com",
                "uploads.namicomi.com",
                "yomumangas.com",
                "api.yomumangas.com",
                "b2.yomumangas.com",
                "weebcentral.com",
                "temp.compsci88.com",
                "xoxocomic.com",
            )
        private val HOST_SUFFIXES = listOf(".mangadex.network", ".hq-now.com", ".planeptune.us", ".bp.blogspot.com")

        fun isSourceHost(host: String): Boolean = host in HOSTS || HOST_SUFFIXES.any { host.endsWith(it) }

        /** "https://x.workers.dev/" → "https://x.workers.dev"; only https, never with a login inside. */
        fun normalize(address: String): String? {
            val text = address.trim().trimEnd('/')
            val url = runCatching { Url(text) }.getOrNull() ?: return null
            val valid = text.startsWith("https://") && url.host.contains('.') && url.user == null
            return text.takeIf { valid }
        }
    }
}

private const val FORBIDDEN = 403

/**
 * Sends every request for a source host through the bridge: `{bridge}/fetch?url=…` with the token; the User-Agent and
 * Referer travel in their own headers, because the browser won't send them (RF-108, RF-109). Without a bridge such a
 * request fails at once and never leaves the browser.
 */
fun HttpClientConfig<*>.routeThroughBridge(bridge: PersonalBridge) {
    install(
        createClientPlugin("Bridge") {
            onRequest { request, _ ->
                if (!PersonalBridge.isSourceHost(request.url.host)) return@onRequest
                val (base, token) = bridge.current() ?: throw SourceException("Precisa da ponte", SourceException.Kind.BRIDGE)
                val target = request.url.buildString()
                request.headers[HttpHeaders.UserAgent]?.let { request.headers[PersonalBridge.UA_HEADER] = it }
                request.headers[HttpHeaders.Referrer]?.let { request.headers[PersonalBridge.REFERER_HEADER] = it }
                request.headers.remove(HttpHeaders.UserAgent)
                request.headers.remove(HttpHeaders.Referrer)
                request.headers[PersonalBridge.TOKEN_HEADER] = token
                request.url.takeFrom("$base/fetch")
                request.url.parameters.clear()
                request.url.parameters.append("url", target)
            }
            // The bridge refuses with 403 (token changed on it, or a host it doesn't allow): that is a bridge problem,
            // not the source's.
            onResponse { response ->
                if (response.call.request.headers[PersonalBridge.TOKEN_HEADER] != null && response.status.value == FORBIDDEN) {
                    // A bridge published before a source existed refuses its host: it needs publishing again.
                    val outdated = response.bodyAsText().contains("\"error\":\"host\"")
                    throw SourceException(
                        if (outdated) "Ponte desatualizada: publique de novo" else "Token recusado pela ponte",
                        SourceException.Kind.BRIDGE,
                    )
                }
            }
        },
    )
}
