package com.tiagohs.hqr.domain.source

import com.tiagohs.hqr.domain.model.SourceId
import kotlinx.coroutines.flow.StateFlow

/**
 * The answer of "Testar" on the bridge screen (RF-108). [OUTDATED]: it works, but a bridge published before the newer
 * sources refuses their hosts.
 */
enum class BridgeCheck { OK, OUTDATED, INVALID_ADDRESS, TOKEN_REFUSED, UNREACHABLE }

/**
 * The user's own bridge (ADR-023): only in the browser, where online sources send no CORS. Without one, those sources
 * stay listed with "Precisa da ponte" and nothing is asked of them (RF-108).
 */
interface Bridge {
    /** The saved address; null when there is none. */
    val address: StateFlow<String?>

    /** Whether [id] can only be reached through the bridge. */
    fun needs(id: SourceId): Boolean

    suspend fun test(
        address: String,
        token: String,
    ): BridgeCheck

    suspend fun save(
        address: String,
        token: String,
    )

    suspend fun clear()
}
