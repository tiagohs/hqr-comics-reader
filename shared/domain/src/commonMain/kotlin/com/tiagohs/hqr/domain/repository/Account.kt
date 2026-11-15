package com.tiagohs.hqr.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Signed in with Google on the personal Worker, or not (RF-119). */
sealed interface AccountState {
    data object SignedOut : AccountState

    data class SignedIn(
        val email: String,
    ) : AccountState
}

/** Why the last cycle failed (RF-125). */
enum class SyncError {
    /** No network or the Worker didn't answer. */
    OFFLINE,

    /** The Worker answered with an error. */
    SERVER,

    /** The session ended (expired or signed out elsewhere): sign in again. */
    SESSION,
}

/** The state shown in Conta e sincronização and, after a day of failures, in the library (RF-125). */
data class SyncStatus(
    val running: Boolean = false,
    val lastSuccessAt: Long? = null,
    val error: SyncError? = null,
    /** Works that came from the account in the last cycle ("4 obras chegaram da conta", RF-124). */
    val arrived: Int = 0,
) {
    /** Whole days without a successful cycle, when a failure lasts more than a day; null otherwise. */
    fun staleDays(now: Long): Int? {
        val since = lastSuccessAt ?: return null
        if (error == null || now - since < DAY_MS) return null
        return ((now - since) / DAY_MS).toInt()
    }

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}

sealed interface SignInResult {
    data class Done(
        val arrived: Int,
    ) : SignInResult

    /** RF-120: the email isn't on the Worker's list. */
    data object NotAllowed : SignInResult

    data object Offline : SignInResult

    data object Failed : SignInResult
}

/** Sign-in and sync with the personal Worker (ADR-027 to ADR-029). */
interface AccountRepository {
    val account: StateFlow<AccountState>
    val status: StateFlow<SyncStatus>

    /** Whether this build knows a Worker; without one the account screen isn't offered. */
    val available: Boolean

    suspend fun signIn(idToken: String): SignInResult

    /** Ends the session; the data stays on the device (RF-119). */
    suspend fun signOut()

    /** One cycle now: sends what changed here and brings what changed elsewhere. */
    suspend fun syncNow()

    /** Keeps the account in sync while the app runs (RF-122): a cycle now and 5 s after each change. Never returns. */
    suspend fun run()
}

/** Asks the platform for a Google ID token: Credential Manager, the browser or the iOS web sheet (ADR-028). */
fun interface GoogleIdTokens {
    /** The token, or null when the user closed the window or the platform can't give one. */
    suspend fun request(): String?

    /** A token that came back on its own (the browser returns from Google's page), taken once. */
    fun pending(): String? = null
}
