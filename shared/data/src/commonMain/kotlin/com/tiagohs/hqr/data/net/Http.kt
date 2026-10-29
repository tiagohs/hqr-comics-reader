package com.tiagohs.hqr.data.net

import com.tiagohs.hqr.domain.source.SourceException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** The platform engine: OkHttp on Android and the JVM, fetch in the browser. */
expect fun httpClient(block: io.ktor.client.HttpClientConfig<*>.() -> Unit = {}): HttpClient

private const val CONNECT_TIMEOUT_MS = 15_000L
private const val REQUEST_TIMEOUT_MS = 60_000L
private const val SOCKET_TIMEOUT_MS = 30_000L
private const val MAX_RETRIES = 3
private const val UNAUTHORIZED = 401
private const val FORBIDDEN = 403
private const val NOT_FOUND = 404
private const val GONE = 410
private const val TOO_MANY = 429
private const val SERVER_ERROR = 500
private const val LAST_SERVER_ERROR = 599

/** The shared client: 3 retries only on GET, with growing waits, and timeouts (ADR-005). */
fun hqrHttpClient(userAgent: String): HttpClient =
    httpClient {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
        install(HttpRequestRetry) {
            maxRetries = MAX_RETRIES
            retryIf { request, response -> request.method == HttpMethod.Get && (response.status.value >= SERVER_ERROR) }
            retryOnExceptionIf { request, cause -> request.method == HttpMethod.Get && cause !is CancellationException }
            exponentialDelay()
        }
        defaultRequest { header(HttpHeaders.UserAgent, userAgent) }
    }

/** Turns transport problems and HTTP statuses into source failures (RF-05). */
fun HttpResponse.ensureOk(): HttpResponse {
    val code = status.value
    if (status.isSuccess()) return this
    throw when (code) {
        UNAUTHORIZED, FORBIDDEN -> SourceException("Acesso recusado ($code)", SourceException.Kind.AUTH)
        NOT_FOUND, GONE -> SourceException("Não encontrado ($code)", SourceException.Kind.NOT_FOUND)
        TOO_MANY -> SourceException("Muitos pedidos", SourceException.Kind.SERVER)
        in SERVER_ERROR..LAST_SERVER_ERROR -> SourceException("Erro da fonte ($code)", SourceException.Kind.SERVER)
        else -> SourceException("Resposta inesperada ($code)", SourceException.Kind.SERVER)
    }
}

suspend inline fun <T> networkCall(block: () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: SourceException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught") e: Exception,
    ) {
        throw SourceException(e.message ?: "Falha de rede", SourceException.Kind.NETWORK, e)
    }

/** GET returning the body bytes, with the source's headers. */
suspend fun HttpClient.bytes(
    url: String,
    headers: Map<String, String> = emptyMap(),
): ByteArray =
    networkCall {
        get(url) { headers.forEach { (k, v) -> header(k, v) } }.ensureOk().bodyAsBytes()
    }
