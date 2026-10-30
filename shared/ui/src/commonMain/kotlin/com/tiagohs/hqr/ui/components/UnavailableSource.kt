package com.tiagohs.hqr.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.domain.source.SourceSwitches
import org.koin.compose.getKoin

/**
 * RF-73: the name of this work's source when the app doesn't have it (a source taken out, or one only another build
 * has), as kept from the backup; null when the source works or the saved ones are still being applied.
 */
@Composable
fun unavailableSourceName(id: SourceId): String? {
    val koin = getKoin()
    val registry = koin.getOrNull<SourceRegistry>()
    val switches = koin.getOrNull<SourceSwitches>()
    val ready by (registry?.isReady ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    // A server added or a list change can bring the source back.
    val version by (registry?.version ?: kotlinx.coroutines.flow.MutableStateFlow(0)).collectAsState()
    val missing = registry != null && ready && id != SourceId.LOCAL && registry[id] == null
    val saved by produceState<String?>(null, id, missing, version) { value = if (missing) switches?.savedName(id) else null }
    return if (missing) saved ?: id.value else null
}
