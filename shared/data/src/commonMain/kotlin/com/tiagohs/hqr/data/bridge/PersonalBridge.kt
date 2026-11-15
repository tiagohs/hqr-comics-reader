package com.tiagohs.hqr.data.bridge

import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.Bridge
import com.tiagohs.hqr.domain.source.SourceException
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.takeFrom
import kotlinx.coroutines.flow.StateFlow

/**
 * The personal Worker's bridge (RF-108, ADR-027): `{base}/fetch`, opened with the session of the account. The client of
 * the web app sends every request for a source host through it (see [routeThroughBridge]).
 */
class PersonalBridge(
    /** The bridge's address: `/bridge` on the app's own origin. */
    private val base: String,
    override val ready: StateFlow<Boolean>,
    private val session: suspend () -> String?,
) : Bridge {
    override fun needs(id: SourceId): Boolean = id in ONLINE

    /** Address and session for a request, or null signed out. */
    suspend fun current(): Pair<String, String>? = session()?.let { base to it }

    companion object {
        const val UA_HEADER = "X-HQR-UA"
        const val REFERER_HEADER = "X-HQR-Referer"

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
         * The hosts behind [ONLINE]; the same list as the Worker's `HOSTS` (hub-worker/). MangaFlix and the
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
    }
}

private const val FORBIDDEN = 403
private const val UNAUTHORIZED = 401

/**
 * Sends every request for a source host through the bridge: `{bridge}/fetch?url=…` with the session; the User-Agent
 * and Referer travel in their own headers, because the browser won't send them (RF-108, RF-109). Signed out, such a
 * request fails at once and never leaves the browser.
 */
fun HttpClientConfig<*>.routeThroughBridge(bridge: PersonalBridge) {
    install(
        createClientPlugin("Bridge") {
            onRequest { request, _ ->
                if (!PersonalBridge.isSourceHost(request.url.host)) return@onRequest
                val (base, token) = bridge.current() ?: throw SourceException("Entre para ler online", SourceException.Kind.BRIDGE)
                val target = request.url.buildString()
                request.headers[HttpHeaders.UserAgent]?.let { request.headers[PersonalBridge.UA_HEADER] = it }
                request.headers[HttpHeaders.Referrer]?.let { request.headers[PersonalBridge.REFERER_HEADER] = it }
                request.headers.remove(HttpHeaders.UserAgent)
                request.headers.remove(HttpHeaders.Referrer)
                request.headers[HttpHeaders.Authorization] = "Bearer $token"
                request.url.takeFrom("$base/fetch")
                request.url.parameters.clear()
                request.url.parameters.append("url", target)
            }
            // The bridge refuses with 401 (the session ended) or 403 (a host it doesn't allow): that is a bridge problem,
            // not the source's.
            onResponse { response ->
                val status = response.status.value
                val throughBridge =
                    response.call.request.url.encodedPath
                        .endsWith("/bridge/fetch") &&
                        response.call.request.headers[HttpHeaders.Authorization] != null
                if (!throughBridge || (status != UNAUTHORIZED && status != FORBIDDEN)) return@onResponse
                // Only the bridge's own refusals are a bridge problem; a 401 or 403 from the source itself passes on (AUTH
                // or a Cloudflare challenge, read by ensureOk).
                val body = response.bodyAsText()
                when {
                    body.contains("\"error\":\"session\"") ->
                        throw SourceException("Entre de novo para ler online", SourceException.Kind.BRIDGE)
                    // A Worker published before a source existed refuses its host: it needs publishing again.
                    body.contains("\"error\":\"host\"") ->
                        throw SourceException("O servidor do HQR ainda não abre esta fonte", SourceException.Kind.BRIDGE)
                    body.contains("\"error\":\"redirect\"") ->
                        throw SourceException("A fonte mudou de endereço", SourceException.Kind.NOT_FOUND)
                }
            }
        },
    )
}
