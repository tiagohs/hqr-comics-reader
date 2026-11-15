package com.tiagohs.hqr.data.sync

import androidx.room3.withWriteTransaction
import com.russhwolf.settings.Settings
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.SyncOutboxEntity
import com.tiagohs.hqr.data.db.SyncStateEntity
import com.tiagohs.hqr.domain.repository.AccountRepository
import com.tiagohs.hqr.domain.repository.AccountState
import com.tiagohs.hqr.domain.repository.SignInResult
import com.tiagohs.hqr.domain.repository.SyncError
import com.tiagohs.hqr.domain.repository.SyncStatus
import com.tiagohs.hqr.domain.source.SecretStore
import com.tiagohs.hqr.domain.sync.SyncChange
import com.tiagohs.hqr.domain.sync.SyncItemId
import com.tiagohs.hqr.domain.sync.SyncKind
import com.tiagohs.hqr.domain.sync.SyncRules
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/**
 * Sign-in and sync with the personal Worker (ADR-027 to ADR-029). A cycle first records what changed here in the
 * outbox (also without internet), then sends the outbox and brings what changed since the cursor.
 */
class SyncAccount(
    private val db: HqrDatabase,
    private val local: SyncLocalData,
    /** Null when this build knows no Worker. */
    private val hub: HubClient?,
    private val secrets: SecretStore,
    private val settings: Settings,
    /** "Android", "iPhone", "Web": shown nowhere, kept with the session. */
    private val device: String,
    private val clock: Clock = Clock.System,
) : AccountRepository {
    private val dao = db.syncDao()
    private val mutex = Mutex()

    private val _account =
        MutableStateFlow(settings.getStringOrNull(KEY_EMAIL)?.let { AccountState.SignedIn(it) } ?: AccountState.SignedOut)
    override val account: StateFlow<AccountState> = _account.asStateFlow()

    private val _status = MutableStateFlow(SyncStatus(lastSuccessAt = settings.getLongOrNull(KEY_LAST_SUCCESS)))
    override val status: StateFlow<SyncStatus> = _status.asStateFlow()

    override val available: Boolean get() = hub != null

    private var arrivedNow = 0

    private val signedIn get() = _account.value is AccountState.SignedIn

    /** The session token, for the bridge on the web (ADR-027). */
    suspend fun token(): String? = if (signedIn) secrets.get(KEY_TOKEN) else null

    override suspend fun signIn(idToken: String): SignInResult {
        val hub = hub ?: return SignInResult.Failed
        return when (val result = hub.signIn(idToken, device)) {
            is HubResult.Ok -> {
                mutex.withLock {
                    db.withWriteTransaction {
                        dao.clearState()
                        dao.clearOutbox()
                    }
                    secrets.put(KEY_TOKEN, result.value.token)
                    settings.putString(KEY_EMAIL, result.value.email)
                    settings.putLong(KEY_CURSOR, 0)
                    settings.putBoolean(KEY_INITIAL, true)
                    _account.value = AccountState.SignedIn(result.value.email)
                }
                // RF-124: the first cycle joins this device with the account.
                cycle(force = true)
                SignInResult.Done(_status.value.arrived)
            }
            HubResult.NotAllowed -> SignInResult.NotAllowed
            HubResult.Offline -> SignInResult.Offline
            HubResult.Session, HubResult.Server -> SignInResult.Failed
        }
    }

    override suspend fun signOut() {
        mutex.withLock {
            val token = secrets.get(KEY_TOKEN)
            secrets.remove(KEY_TOKEN)
            listOf(KEY_EMAIL, KEY_CURSOR, KEY_INITIAL, KEY_LAST_SUCCESS).forEach(settings::remove)
            db.withWriteTransaction {
                dao.clearState()
                dao.clearOutbox()
            }
            _account.value = AccountState.SignedOut
            _status.value = SyncStatus()
            // The data stays on the device; telling the Worker is best effort.
            if (token != null) hub?.signOut(token)
        }
    }

    override suspend fun syncNow() = cycle(force = true)

    /**
     * RF-122: a cycle at start, then 5 s after the last change to the account's tables the change is recorded and, when
     * there is something to send, sent. The app's own writes from a sync leave nothing to send, so they end there.
     */
    @OptIn(FlowPreview::class)
    @Suppress("SpreadOperator") // Room takes the table names as varargs; a dozen names, once per start.
    override suspend fun run() {
        if (signedIn) cycle(force = true)
        db.invalidationTracker
            .createFlow(*WATCHED, emitInitialState = false)
            .debounce(CHANGE_DELAY_MS)
            .collect { if (signedIn) cycle(force = false) }
    }

    /** One cycle; without [force], the network is used only when the outbox has something. */
    private suspend fun cycle(force: Boolean) {
        val hub = hub ?: return
        mutex.withLock {
            if (!signedIn) return
            capture()
            if (!force && dao.outbox().isEmpty()) return
            _status.update { it.copy(running = true) }
            val error = exchange(hub)
            val now = clock.now().toEpochMilliseconds()
            if (error == null) {
                settings.putLong(KEY_LAST_SUCCESS, now)
                settings.putBoolean(KEY_INITIAL, false)
            }
            _status.update {
                it.copy(
                    running = false,
                    error = error,
                    lastSuccessAt =
                        if (error ==
                            null
                        ) {
                            now
                        } else {
                            it.lastSuccessAt
                        },
                    arrived = if (error == null) arrivedNow else 0,
                )
            }
        }
    }

    /** Records in the outbox what changed here since the last cycle, and makes that the new base. */
    private suspend fun capture() {
        val now = clock.now().toEpochMilliseconds()
        db.withWriteTransaction {
            val base = snapshot()
            val current = local.current(base)
            val changes = SyncRules.diff(base, current, now, initial = settings.getBoolean(KEY_INITIAL, false))
            if (changes.isEmpty()) return@withWriteTransaction
            dao.enqueue(changes.map { SyncOutboxEntity(it.kind.wire, it.key, it.payload, it.changedAt, it.deleted) })
            dao.remember(changes.filterNot { it.deleted }.map { SyncStateEntity(it.kind.wire, it.key, it.payload) })
            changes.filter { it.deleted }.forEach { dao.forget(it.kind.wire, it.key) }
        }
    }

    /**
     * Brings what changed elsewhere first and merges it here, then records the merge and sends the outbox: a chapter
     * read on another device is never overwritten by this device's older idea of it (RF-123).
     */
    private suspend fun exchange(hub: HubClient): SyncError? {
        arrivedNow = 0
        repeat(MAX_PAGES) {
            when (val result = send(hub, emptyList())) {
                is Outcome.Failed -> return result.error
                is Outcome.Done -> if (!result.value) return push(hub)
            }
        }
        return push(hub)
    }

    private suspend fun push(hub: HubClient): SyncError? {
        capture()
        val outbox = dao.outbox()
        if (outbox.isEmpty()) return null
        return (send(hub, outbox) as? Outcome.Failed)?.error
    }

    /** One request; returns whether there is more to bring, or the error. */
    private suspend fun send(
        hub: HubClient,
        outbox: List<SyncOutboxEntity>,
    ): Outcome<Boolean> {
        val sent = outbox.mapNotNull { o -> SyncKind.of(o.kind)?.let { SyncChange(it, o.key, o.payload, o.changedAt, o.deleted) } }
        val result =
            secrets.get(KEY_TOKEN)?.let { hub.sync(it, settings.getLong(KEY_CURSOR, 0), sent) } ?: HubResult.Session
        val page =
            (result as? HubResult.Ok)?.value ?: return Outcome.Failed(
                when (result) {
                    HubResult.Session -> SyncError.SESSION
                    HubResult.Offline -> SyncError.OFFLINE
                    else -> SyncError.SERVER
                },
            )
        val now = clock.now().toEpochMilliseconds()
        db.withWriteTransaction {
            outbox.forEach { dao.sent(it.kind, it.key, it.changedAt) }
            // A newer change of this device still waiting in the outbox wins over the account's older version.
            val pending = dao.outbox().associate { SyncItemId(SyncKind.of(it.kind) ?: SyncKind.SETTING, it.key) to it.changedAt }
            val incoming = page.changes.filter { c -> c.kind == SyncKind.CHAPTER || (pending[c.id] ?: Long.MIN_VALUE) <= c.changedAt }
            arrivedNow += local.apply(incoming, snapshot(), now)
            // The base becomes what the account has; what the merge changed here goes out in the next capture.
            dao.remember(incoming.filterNot { it.deleted }.map { SyncStateEntity(it.kind.wire, it.key, it.payload) })
            incoming.filter { it.deleted }.forEach { dao.forget(it.kind.wire, it.key) }
        }
        settings.putLong(KEY_CURSOR, page.cursor)
        return Outcome.Done(page.more)
    }

    private sealed interface Outcome<out T> {
        data class Done<T>(
            val value: T,
        ) : Outcome<T>

        data class Failed(
            val error: SyncError,
        ) : Outcome<Nothing>
    }

    private suspend fun snapshot(): Map<SyncItemId, String> =
        dao.state().mapNotNull { s -> SyncKind.of(s.kind)?.let { SyncItemId(it, s.key) to s.payload } }.toMap()

    companion object {
        const val CHANGE_DELAY_MS = 5_000L
        private const val MAX_PAGES = 40
        private const val KEY_TOKEN = "sync.session"
        private const val KEY_EMAIL = "sync.email"
        private const val KEY_CURSOR = "sync.cursor"
        private const val KEY_INITIAL = "sync.initial"
        private const val KEY_LAST_SUCCESS = "sync.lastSuccess"

        /** The tables behind the account's items. */
        private val WATCHED =
            arrayOf(
                "manga",
                "chapter",
                "progress",
                "history",
                "category",
                "manga_category",
                "manga_reader_settings",
                "saved_search",
                "feed_item",
                "server",
            )
    }
}
