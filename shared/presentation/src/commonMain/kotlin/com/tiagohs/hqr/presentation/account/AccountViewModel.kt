package com.tiagohs.hqr.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.repository.AccountRepository
import com.tiagohs.hqr.domain.repository.AccountState
import com.tiagohs.hqr.domain.repository.GoogleIdTokens
import com.tiagohs.hqr.domain.repository.SignInResult
import com.tiagohs.hqr.domain.repository.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Why "Entrar com o Google" didn't work (RF-119, RF-120). */
enum class SignInProblem { NOT_ALLOWED, OFFLINE, FAILED }

/** SCR-SET-005 Conta e sincronização, and the library's notice (RF-119 to RF-125). */
class AccountViewModel(
    private val repository: AccountRepository,
    private val tokens: GoogleIdTokens?,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    data class State(
        val account: AccountState = AccountState.SignedOut,
        val status: SyncStatus = SyncStatus(),
        val signingIn: Boolean = false,
        val problem: SignInProblem? = null,
        /** "N obras chegaram da conta", shown once (RF-124). */
        val arrived: Int? = null,
    )

    private val local = MutableStateFlow(State())

    val state: StateFlow<State> =
        combine(local, repository.account, repository.status) { l, account, status ->
            l.copy(account = account, status = status)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, State(repository.account.value, repository.status.value))

    /** Whether this build can sign in at all (a Worker and a way to get the Google token). */
    val available: Boolean get() = repository.available && tokens != null

    fun signIn() {
        val tokens = tokens ?: return
        if (local.value.signingIn) return
        local.update { it.copy(signingIn = true, problem = null) }
        viewModelScope.launch {
            val token = tokens.request()
            if (token == null) {
                local.update { it.copy(signingIn = false) }
                return@launch
            }
            signInWith(token)
        }
    }

    /** The web gets the token back from Google's page when the app opens again. */
    fun signInWith(idToken: String) {
        local.update { it.copy(signingIn = true, problem = null) }
        viewModelScope.launch {
            val result = repository.signIn(idToken)
            local.update {
                when (result) {
                    is SignInResult.Done -> it.copy(signingIn = false, arrived = result.arrived.takeIf { n -> n > 0 })
                    SignInResult.NotAllowed -> it.copy(signingIn = false, problem = SignInProblem.NOT_ALLOWED)
                    SignInResult.Offline -> it.copy(signingIn = false, problem = SignInProblem.OFFLINE)
                    SignInResult.Failed -> it.copy(signingIn = false, problem = SignInProblem.FAILED)
                }
            }
        }
    }

    /** The browser comes back from Google's page with the token: the account screen finishes the sign-in. */
    fun finishPendingSignIn() {
        tokens?.pending()?.let(::signInWith)
    }

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }

    fun syncNow() {
        viewModelScope.launch {
            repository.syncNow()
            val arrived = repository.status.value.arrived
            if (arrived > 0) local.update { it.copy(arrived = arrived) }
        }
    }

    fun arrivedShown() = local.update { it.copy(arrived = null) }

    /** RF-125: days without syncing, only when signed in and failing for more than a day. */
    fun staleDays(state: State): Int? =
        if (state.account is AccountState.SignedIn) state.status.staleDays(clock.now().toEpochMilliseconds()) else null
}
