package com.tiagohs.hqr.domain.source

import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import kotlinx.coroutines.flow.Flow

/** A server the user added (RF-17); the password never leaves the [SecretStore]. */
data class Server(
    val id: SourceId,
    val name: String,
    val url: String,
    val username: String?,
)

/** What the user typed in SCR-SRV-001. */
data class ServerInput(
    val name: String,
    val url: String,
    val username: String = "",
    val password: String = "",
)

/** RF-19 and RF-95: why a server can't be used, each with its own message. */
enum class ServerProblem {
    INVALID_ADDRESS,
    CREDENTIALS_IN_ADDRESS,
    CLEARTEXT_OUTSIDE_HOME,
    UNREACHABLE,
    WRONG_LOGIN,
    CERTIFICATE,
    NOT_OPDS,
}

/** Add, test and remove the user's OPDS servers; each one becomes a source (RF-17 to RF-19). */
interface ServerManager {
    fun observe(): Flow<List<Server>>

    /**
     * Null when the server answers as an OPDS catalog with these credentials. Editing [id] with the password blank
     * uses the saved one, but only for the same address origin (RF-95).
     */
    suspend fun test(
        input: ServerInput,
        id: SourceId? = null,
    ): ServerProblem?

    /** Tests, then saves; a problem means nothing was saved. [id] edits an existing server. */
    suspend fun save(
        input: ServerInput,
        id: SourceId? = null,
    ): ServerProblem?

    suspend fun delete(id: SourceId)

    /** Registers the saved servers as sources; once per start. */
    suspend fun start()
}

/** One screen of a server's catalog (SCR-SRV-002, RF-18). */
data class CatalogPage(
    val title: String,
    val entries: List<CatalogEntry>,
    val nextUrl: String? = null,
    val canSearch: Boolean = false,
    /** The address of this page itself. */
    val url: String? = null,
)

/** A folder to enter or a book to read. */
data class CatalogEntry(
    val title: String,
    val url: String,
    val isBook: Boolean,
    val coverUrl: String? = null,
    val subtitle: String? = null,
)

/** Navigation of an OPDS source: feeds, OpenSearch, and the feed of a book as a work (RF-18). */
interface CatalogBrowser : Source {
    /** The root feed when [url] is null. */
    suspend fun feed(url: String?): CatalogPage

    suspend fun searchCatalog(query: String): CatalogPage

    /** The work that holds the books of the feed at [feedUrl]. */
    fun work(
        feedUrl: String,
        title: String,
    ): SourceManga
}

/** Passwords and tokens in the system's secure storage: Android Keystore, browser storage (architecture §3). */
interface SecretStore {
    suspend fun get(key: String): String?

    suspend fun put(
        key: String,
        value: String,
    )

    suspend fun remove(key: String)
}
