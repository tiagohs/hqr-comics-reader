package com.tiagohs.hqr.domain.source

import com.tiagohs.hqr.domain.model.SourceId
import kotlinx.coroutines.flow.StateFlow

/**
 * The way to the online sources in the browser, where they send no CORS: the personal Worker's bridge, open only with
 * the account's session (ADR-027). Signed out, those sources stay listed with "Entre para ler online" and nothing is
 * asked of them (RF-108).
 */
interface Bridge {
    /** True while the browser is signed in, so the bridge answers. */
    val ready: StateFlow<Boolean>

    /** Whether [id] can only be reached through the bridge. */
    fun needs(id: SourceId): Boolean
}
