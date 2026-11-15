package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.domain.repository.AccountRepository
import com.tiagohs.hqr.domain.repository.AccountState
import com.tiagohs.hqr.domain.repository.GoogleIdTokens
import com.tiagohs.hqr.domain.repository.SignInResult
import com.tiagohs.hqr.domain.repository.SyncError
import com.tiagohs.hqr.domain.repository.SyncStatus
import com.tiagohs.hqr.presentation.account.AccountViewModel
import com.tiagohs.hqr.presentation.account.SignInProblem
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

class AccountViewModelTest : MainDispatcherTest() {
    private class FakeAccount : AccountRepository {
        override val account = MutableStateFlow<AccountState>(AccountState.SignedOut)
        override val status = MutableStateFlow(SyncStatus())
        override val available = true
        var result: SignInResult = SignInResult.Done(4)
        var tokens = mutableListOf<String>()
        var syncs = 0

        override suspend fun signIn(idToken: String): SignInResult {
            tokens += idToken
            if (result is SignInResult.Done) account.value = AccountState.SignedIn("tiago@example.com")
            return result
        }

        override suspend fun signOut() {
            account.value = AccountState.SignedOut
        }

        override suspend fun syncNow() {
            syncs++
        }

        override suspend fun run() = Unit
    }

    private val repo = FakeAccount()
    private val day = 86_400_000L
    private val clock =
        object : Clock {
            override fun now() = Instant.fromEpochMilliseconds(3 * day)
        }

    private fun vm(tokens: GoogleIdTokens? = GoogleIdTokens { "id-token" }) = track(AccountViewModel(repo, tokens, clock))

    @Test
    fun RF119_RF124_signing_in_shows_the_email_and_what_came_from_the_account() =
        runBlocking<Unit> {
            val vm = vm()
            vm.signIn()
            waitFor { vm.state.value.account is AccountState.SignedIn && !vm.state.value.signingIn }
            repo.tokens shouldBe listOf("id-token")
            vm.state.value.arrived shouldBe 4
            vm.arrivedShown()
            waitFor { vm.state.value.arrived == null }
        }

    @Test
    fun RF119_the_browser_finishes_the_sign_in_with_the_token_it_brought_back() =
        runBlocking<Unit> {
            var pending: String? = "from-google"
            val vm =
                vm(
                    object : GoogleIdTokens {
                        override suspend fun request(): String? = null

                        override fun pending(): String? = pending.also { pending = null }
                    },
                )
            vm.finishPendingSignIn()
            waitFor { vm.state.value.account is AccountState.SignedIn }
            vm.finishPendingSignIn()
            repo.tokens shouldBe listOf("from-google")
        }

    @Test
    fun RF120_an_account_outside_the_list_shows_the_reason() =
        runBlocking<Unit> {
            repo.result = SignInResult.NotAllowed
            val vm = vm()
            vm.signIn()
            waitFor { vm.state.value.problem == SignInProblem.NOT_ALLOWED }
            vm.state.value.account shouldBe AccountState.SignedOut
        }

    @Test
    fun RF119_closing_the_google_window_changes_nothing() =
        runBlocking<Unit> {
            val vm = vm(GoogleIdTokens { null })
            vm.signIn()
            waitFor { !vm.state.value.signingIn }
            repo.tokens shouldBe emptyList()
            vm.state.value.problem shouldBe null
        }

    @Test
    fun RF119_without_a_way_to_sign_in_the_account_is_not_offered() {
        vm(tokens = null).available shouldBe false
        vm().available shouldBe true
    }

    @Test
    fun RF125_the_library_notice_appears_after_two_days_without_syncing() =
        runBlocking<Unit> {
            val vm = vm()
            repo.account.value = AccountState.SignedIn("tiago@example.com")
            repo.status.value = SyncStatus(lastSuccessAt = day - 1, error = SyncError.OFFLINE)
            waitFor { vm.state.value.status.error != null }
            vm.staleDays(vm.state.value) shouldBe 2
            repo.account.value = AccountState.SignedOut
            waitFor { vm.state.value.account == AccountState.SignedOut }
            vm.staleDays(vm.state.value) shouldBe null
        }

    @Test
    fun RF122_sync_now_and_sign_out_reach_the_account() =
        runBlocking<Unit> {
            val vm = vm()
            repo.account.value = AccountState.SignedIn("tiago@example.com")
            vm.syncNow()
            waitFor { repo.syncs == 1 }
            vm.signOut()
            waitFor { vm.state.value.account == AccountState.SignedOut }
        }
}
