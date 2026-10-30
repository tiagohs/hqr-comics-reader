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
) : ServerManager {
    private val dao = db.serverDao()

    override fun observe(): Flow<List<Server>> = dao.observe().map { list -> list.map { it.toServer() } }

    override suspend fun start() {
        dao.list().forEach { register(it.toServer()) }
    }

    override suspend fun test(input: ServerInput): ServerProblem? {
        validate(input)?.let { return it }
        val probe =
            OpdsSource(Server(SourceId("probe"), input.name, input.url.trim(), input.username.ifBlank { null }), http) { input.password }
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
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun save(
        input: ServerInput,
        id: SourceId?,
    ): ServerProblem? {
        // Editing without typing the password again keeps the saved one.
        val typed = if (id != null && input.password.isEmpty()) input.copy(password = secrets.get(secretKey(id)).orEmpty()) else input
        test(typed)?.let { return it }
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
        registry.register(OpdsSource(server, http) { secrets.get(secretKey(server.id)) })
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
                else -> null
            }
        }
    }
}
