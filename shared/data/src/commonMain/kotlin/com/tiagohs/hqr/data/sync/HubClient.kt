package com.tiagohs.hqr.data.sync

import com.tiagohs.hqr.domain.sync.SyncChange
import com.tiagohs.hqr.domain.sync.SyncKind
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The answer of a call to the Worker. */
sealed interface HubResult<out T> {
    data class Ok<T>(
        val value: T,
    ) : HubResult<T>

    /** 401: the session is gone. */
    data object Session : HubResult<Nothing>

    /** 403 on sign-in: the email isn't on the list (RF-120). */
    data object NotAllowed : HubResult<Nothing>

    data object Offline : HubResult<Nothing>

    data object Server : HubResult<Nothing>
}

@Serializable
data class HubSession(
    val token: String,
    val email: String,
    val expiresAt: Long,
)

data class HubPage(
    val cursor: Long,
    val changes: List<SyncChange>,
    val more: Boolean,
)

/** The personal Worker's API (ADR-027): `/api/v1/auth/google`, `/api/v1/auth/signout` and `/api/v1/sync`. */
class HubClient(
    private val http: HttpClient,
    /** The Worker's address, without a trailing slash; "" on the web, where the app and the API share the origin. */
    private val baseUrl: String,
) {
    suspend fun signIn(
        idToken: String,
        device: String,
    ): HubResult<HubSession> =
        call("/api/v1/auth/google", token = null, body = json.encodeToString(SignInBody.serializer(), SignInBody(idToken, device))) {
            json.decodeFromString(HubSession.serializer(), it)
        }

    suspend fun signOut(token: String): HubResult<Unit> = call("/api/v1/auth/signout", token, "{}") { }

    suspend fun sync(
        token: String,
        cursor: Long,
        changes: List<SyncChange>,
    ): HubResult<HubPage> {
        val body = SyncBody(cursor, changes.map { WireChange(it.kind.wire, it.key, it.payload, it.changedAt, it.deleted) })
        return call("/api/v1/sync", token, json.encodeToString(SyncBody.serializer(), body)) { text ->
            val page = json.decodeFromString(SyncAnswer.serializer(), text)
            HubPage(
                page.cursor,
                page.changes.mapNotNull { c -> SyncKind.of(c.kind)?.let { SyncChange(it, c.key, c.payload, c.changedAt, c.deleted) } },
                page.more,
            )
        }
    }

    private suspend fun <T> call(
        path: String,
        token: String?,
        body: String,
        read: (String) -> T,
    ): HubResult<T> =
        try {
            val response =
                http.post(baseUrl + path) {
                    contentType(ContentType.Application.Json)
                    token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                    setBody(body)
                }
            when (response.status.value) {
                OK -> HubResult.Ok(read(response.bodyAsText()))
                UNAUTHORIZED -> if (token == null) HubResult.Server else HubResult.Session
                FORBIDDEN -> HubResult.NotAllowed
                else -> HubResult.Server
            }
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
        ) {
            HubResult.Offline
        }

    @Serializable
    private data class SignInBody(
        val idToken: String,
        val device: String,
    )

    @Serializable
    private data class WireChange(
        val kind: String,
        val key: String,
        val payload: String,
        val changedAt: Long,
        val deleted: Boolean,
    )

    @Serializable
    private data class SyncBody(
        val cursor: Long,
        val changes: List<WireChange>,
    )

    @Serializable
    private data class SyncAnswer(
        val cursor: Long,
        val changes: List<WireChange>,
        val more: Boolean,
    )

    private companion object {
        const val OK = 200
        const val UNAUTHORIZED = 401
        const val FORBIDDEN = 403
        val json = Json { ignoreUnknownKeys = true }
    }
}
