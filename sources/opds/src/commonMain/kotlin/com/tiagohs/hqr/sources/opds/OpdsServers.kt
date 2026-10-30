package com.tiagohs.hqr.sources.opds

import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.ServerEntity
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.SecretStore
import com.tiagohs.hqr.domain.source.Server
import com.tiagohs.hqr.domain.source.ServerInput
import com.tiagohs.hqr.domain.source.ServerManager
import com.tiagohs.hqr.domain.source.ServerProblem
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceRegistry
import io.ktor.client.HttpClient
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The user's servers (RF-17 to RF-19, RF-95): saved in the `server` table, password in the [SecretStore] (never in the
 * database nor the backup, RF-72), each one registered as an [OpdsSource].
 */
class OpdsServers(
    db: HqrDatabase,
    private val secrets: SecretStore,
    private val registry: SourceRegistry,
    private val http: HttpClient,
    private val books: BookStore = MemoryBookStore(),
) : ServerManager {
    private val dao = db.serverDao()

    override fun observe(): Flow<List<Server>> = dao.observe().map { list -> list.map { it.toServer() } }

    override suspend fun start() {
        dao.list().forEach { register(it.toServer()) }
    }

    override suspend fun test(
        input: ServerInput,
        id: SourceId?,
    ): ServerProblem? {
        validate(input)?.let { return it }
        val typed = withSavedPassword(input, id)
        val probe =
            OpdsSource(
                Server(SourceId("probe"), typed.name, typed.url.trim(), typed.username.ifBlank { null }),
                http,
                { typed.password },
                books,
            )
        return try {
            probe.check()
            null
        } catch (e: SourceException) {
            when (e.kind) {
                SourceException.Kind.AUTH -> ServerProblem.WRONG_LOGIN
                SourceException.Kind.CERTIFICATE -> ServerProblem.CERTIFICATE
                SourceException.Kind.PARSE, SourceException.Kind.NOT_FOUND -> ServerProblem.NOT_OPDS
                else -> ServerProblem.UNREACHABLE
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
        ) {
            // Whatever else goes wrong talking to the address, the form says so instead of closing the app.
            ServerProblem.UNREACHABLE
        }
    }

    /** The saved password is reused only when the address keeps its origin; a new host has to get it typed (RF-95). */
    private suspend fun withSavedPassword(
        input: ServerInput,
        id: SourceId?,
    ): ServerInput {
        if (id == null || input.password.isNotEmpty()) return input
        val saved = dao.list().firstOrNull { it.sourceId == id.value } ?: return input
        val sameOrigin = runCatching { OpdsSource.origin(saved.url) == OpdsSource.origin(input.url.trim()) }.getOrDefault(false)
        val sameUser = saved.username.orEmpty() == input.username.trim()
        return if (sameOrigin && sameUser) input.copy(password = secrets.get(secretKey(id)).orEmpty()) else input
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun save(
        input: ServerInput,
        id: SourceId?,
    ): ServerProblem? {
        test(input, id)?.let { return it }
        // Editing without typing the password again keeps the saved one, for the same origin only.
        val typed = withSavedPassword(input, id)
        val sourceId = id ?: SourceId("$PREFIX${Uuid.random()}")
        val server =
            Server(
                sourceId,
                input.name.trim().ifBlank { Url(input.url.trim()).host },
                input.url.trim(),
                input.username.trim().ifBlank { null },
            )
        dao.upsert(ServerEntity(sourceId.value, KIND, server.name, server.url, server.username, pinnedCertificate = null))
        if (typed.password.isNotEmpty()) secrets.put(secretKey(sourceId), typed.password) else secrets.remove(secretKey(sourceId))
        register(server)
        return null
    }

    override suspend fun delete(id: SourceId) {
        dao.delete(id.value)
        secrets.remove(secretKey(id))
        registry.unregister(id)
    }

    private fun register(server: Server) {
        registry.register(OpdsSource(server, http, { secrets.get(secretKey(server.id)) }, books))
    }

    private fun ServerEntity.toServer() = Server(SourceId(sourceId), name, url, username)

    companion object {
        const val PREFIX = "opds-"
        private const val KIND = "opds"

        fun secretKey(id: SourceId) = "server.password.${id.value}"

        /** RF-95: only http and https, and never a login written in the address. */
        fun validate(input: ServerInput): ServerProblem? {
            val text = input.url.trim()
            val url = runCatching { Url(text) }.getOrNull()
            val hasLogin = url?.user != null || url?.password != null || text.substringAfter("://").substringBefore('/').contains('@')
            return when {
                !text.startsWith("http://") && !text.startsWith("https://") -> ServerProblem.INVALID_ADDRESS
                url == null || url.host.isBlank() -> ServerProblem.INVALID_ADDRESS
                hasLogin -> ServerProblem.CREDENTIALS_IN_ADDRESS
                // Architecture §6: plain http only inside the home network, so a login never crosses the internet unencrypted.
                text.startsWith("http://") && !isHomeNetwork(url.host) -> ServerProblem.CLEARTEXT_OUTSIDE_HOME
                else -> null
            }
        }

        /** localhost, private ranges (RFC 1918, link-local), .local/.lan/.home.arpa and names without a dot. */
        fun isHomeNetwork(host: String): Boolean {
            val h = host.lowercase().trim('[', ']')
            val parts = h.split('.').mapNotNull { it.toIntOrNull() }
            val ipv4 = parts.size == IPV4_PARTS && h.count { it == '.' } == IPV4_PARTS - 1
            return when {
                h == "localhost" || h == "::1" -> true
                ipv4 ->
                    parts[0] == PRIVATE_A ||
                        parts[0] == LOOPBACK ||
                        (parts[0] == PRIVATE_C_1 && parts[1] == PRIVATE_C_2) ||
                        (parts[0] == PRIVATE_B && parts[1] in PRIVATE_B_RANGE) ||
                        (parts[0] == LINK_1 && parts[1] == LINK_2)
                else -> HOME_SUFFIXES.any { h.endsWith(it) } || !h.contains('.')
            }
        }

        private const val IPV4_PARTS = 4
        private const val PRIVATE_A = 10
        private const val LOOPBACK = 127
        private const val PRIVATE_C_1 = 192
        private const val PRIVATE_C_2 = 168
        private const val PRIVATE_B = 172
        private val PRIVATE_B_RANGE = 16..31
        private const val LINK_1 = 169
        private const val LINK_2 = 254
        private val HOME_SUFFIXES = listOf(".local", ".lan", ".home.arpa", ".internal")
    }
}
