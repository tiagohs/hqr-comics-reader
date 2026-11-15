package com.tiagohs.hqr.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.domain.repository.AccountRepository
import com.tiagohs.hqr.domain.repository.AccountState
import com.tiagohs.hqr.domain.repository.GoogleIdTokens
import com.tiagohs.hqr.domain.repository.SignInResult
import com.tiagohs.hqr.domain.repository.SyncError
import com.tiagohs.hqr.domain.repository.SyncStatus
import com.tiagohs.hqr.ui.navigation.HqrApp
import com.tiagohs.hqr.ui.settings.AccountScreen
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test

/** A stand-in for the Worker account: signs in the allowed email, counts the cycles. */
class FakeAccount(
    private val allowed: String = "tiago@example.com",
) : AccountRepository {
    override val account = MutableStateFlow<AccountState>(AccountState.SignedOut)
    override val status = MutableStateFlow(SyncStatus())
    override val available = true
    var syncs = 0

    override suspend fun signIn(idToken: String): SignInResult {
        if (idToken != allowed) return SignInResult.NotAllowed
        account.value = AccountState.SignedIn(idToken)
        status.value = SyncStatus(lastSuccessAt = System.currentTimeMillis(), arrived = 4)
        return SignInResult.Done(4)
    }

    override suspend fun signOut() {
        account.value = AccountState.SignedOut
    }

    override suspend fun syncNow() {
        syncs++
    }

    override suspend fun run() = Unit
}

/** SCR-SET-005 and the library's notice (RF-119 to RF-125). */
@OptIn(ExperimentalTestApi::class)
class AccountScreensTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    private fun ComposeUiTest.waitFor(matcher: SemanticsMatcher) {
        runCatching { waitUntil(timeoutMillis = 10_000) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() } }
            .onFailure {
                onRoot().printToLog("TREE")
                throw it
            }
    }

    private fun koin(
        account: FakeAccount,
        token: String = "tiago@example.com",
        onboarded: Boolean = true,
    ) = startTestKoin(
        onboarded = onboarded,
        extra =
            module {
                single<AccountRepository> { account }
                single<GoogleIdTokens> { GoogleIdTokens { token } }
            },
    )

    @Test
    fun RF119_RF124_signing_in_shows_the_account_and_what_came_from_it() =
        screenTest {
            val account = FakeAccount()
            koin(account)
            setAppContent { AccountScreen(onBack = {}) }
            waitFor(hasText("Sign in with Google"))
            screenshot("demo-SCR-SET-005-signed-out")
            onAllNodes(hasText("Sign in with Google"))[0].performClick()
            waitFor(hasText("tiago@example.com"))
            waitFor(hasText("4 works came from the account"))
            waitFor(hasText("Synced just now"))
            screenshot("demo-SCR-SET-005-synced")
            onAllNodes(hasText("Sync now"))[0].performClick()
            waitUntil(timeoutMillis = 10_000) { account.syncs == 1 }
            onAllNodes(hasText("Sign out"))[0].performClick()
            waitFor(hasText("Sign in with Google"))
        }

    @Test
    fun RF120_an_account_outside_the_list_sees_why() =
        screenTest {
            koin(FakeAccount(), token = "someone@example.com")
            setAppContent { AccountScreen(onBack = {}) }
            waitFor(hasText("Sign in with Google"))
            onAllNodes(hasText("Sign in with Google"))[0].performClick()
            waitFor(hasText("This account has no access to this HQR"))
            waitFor(hasText("Use another account"))
            screenshot("demo-SCR-SET-005-not-allowed")
        }

    @Test
    fun RF125_failures_show_the_reason_and_try_again() =
        screenTest {
            val account = FakeAccount()
            account.account.value = AccountState.SignedIn("tiago@example.com")
            account.status.value = SyncStatus(lastSuccessAt = 0, error = SyncError.OFFLINE)
            koin(account)
            setAppContent { AccountScreen(onBack = {}) }
            waitFor(hasText("Couldn't sync"))
            screenshot("demo-SCR-SET-005-error")
            onAllNodes(hasText("Try again"))[0].performClick()
            waitUntil(timeoutMillis = 10_000) { account.syncs == 1 }
        }

    @Test
    fun RF125_the_library_shows_a_failure_older_than_a_day_and_opens_the_account() =
        screenTest {
            val account = FakeAccount()
            account.account.value = AccountState.SignedIn("tiago@example.com")
            account.status.value =
                SyncStatus(lastSuccessAt = System.currentTimeMillis() - 2 * 86_400_000L - 1000, error = SyncError.OFFLINE)
            val db = koin(account)
            kotlinx.coroutines.runBlocking { libraryWork(db) }
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("2 days without syncing"))
            screenshot("demo-SCR-LIB-001-sync-notice")
            onAllNodes(hasText("See"))[0].performClick()
            waitFor(hasText("Couldn't sync"))
        }

    @Test
    fun RF119_the_first_opening_offers_the_account_and_lets_it_wait() =
        screenTest {
            koin(FakeAccount(), onboarded = false)
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Start reading"))
            onAllNodes(hasText("Start reading"))[0].performClick()
            waitFor(hasText("Not now"))
            screenshot("demo-SCR-ONB-001-account")
            onAllNodes(hasText("Not now"))[0].performClick()
            waitFor(hasText("Library"))
            org.koin.core.context.GlobalContext
                .get()
                .get<AppPreferences>()
                .onboarded.value shouldBe true
        }

    @Test
    fun RF119_settings_lead_to_the_account() =
        screenTest {
            koin(FakeAccount())
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Settings"))
            onAllNodes(hasText("Settings"))[0].performClick()
            waitFor(hasText("Not signed in"))
            onAllNodes(hasText("Account and sync"))[0].performClick()
            waitFor(hasText("Sign in with Google"))
        }
}

private infix fun <T> T.shouldBe(expected: T) = check(this == expected) { "expected $expected but was $this" }

private suspend fun libraryWork(db: com.tiagohs.hqr.data.db.HqrDatabase) {
    val mangas =
        com.tiagohs.hqr.data.repository
            .RoomMangaRepository(db, kotlin.time.Clock.System)
    val manga =
        mangas.upsert(
            com.tiagohs.hqr.domain.model
                .SourceManga(
                    com.tiagohs.hqr.domain.model
                        .SourceId("demo"),
                    "kaiju",
                    "Kaiju Express",
                ),
        )
    mangas.setInLibrary(manga.id, true)
}
