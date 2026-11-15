package com.tiagohs.hqr.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.repository.AccountState
import com.tiagohs.hqr.domain.repository.SyncError
import com.tiagohs.hqr.domain.repository.SyncStatus
import com.tiagohs.hqr.presentation.account.AccountViewModel
import com.tiagohs.hqr.presentation.account.SignInProblem
import com.tiagohs.hqr.ui.components.ComicTitle
import com.tiagohs.hqr.ui.components.halftoneBehind
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.account_goes
import com.tiagohs.hqr.ui.resources.account_goes_feed
import com.tiagohs.hqr.ui.resources.account_goes_library
import com.tiagohs.hqr.ui.resources.account_goes_progress
import com.tiagohs.hqr.ui.resources.account_google
import com.tiagohs.hqr.ui.resources.account_not_allowed
import com.tiagohs.hqr.ui.resources.account_not_allowed_hint
import com.tiagohs.hqr.ui.resources.account_offline_note
import com.tiagohs.hqr.ui.resources.account_pitch
import com.tiagohs.hqr.ui.resources.account_pitch_title
import com.tiagohs.hqr.ui.resources.account_sign_in
import com.tiagohs.hqr.ui.resources.account_sign_in_failed
import com.tiagohs.hqr.ui.resources.account_sign_in_offline
import com.tiagohs.hqr.ui.resources.account_sign_out
import com.tiagohs.hqr.ui.resources.account_sign_out_note
import com.tiagohs.hqr.ui.resources.account_stays
import com.tiagohs.hqr.ui.resources.account_stays_device
import com.tiagohs.hqr.ui.resources.account_stays_downloads
import com.tiagohs.hqr.ui.resources.account_title
import com.tiagohs.hqr.ui.resources.account_use_other
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.sync_arrived
import com.tiagohs.hqr.ui.resources.sync_done_hours
import com.tiagohs.hqr.ui.resources.sync_done_minutes
import com.tiagohs.hqr.ui.resources.sync_done_now
import com.tiagohs.hqr.ui.resources.sync_failed
import com.tiagohs.hqr.ui.resources.sync_failed_offline
import com.tiagohs.hqr.ui.resources.sync_failed_server
import com.tiagohs.hqr.ui.resources.sync_failed_session
import com.tiagohs.hqr.ui.resources.sync_never
import com.tiagohs.hqr.ui.resources.sync_now
import com.tiagohs.hqr.ui.resources.sync_running
import com.tiagohs.hqr.ui.resources.sync_running_hint
import com.tiagohs.hqr.ui.resources.sync_try_again
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 3_600_000L

/** SCR-SET-005 Conta e sincronização (RF-119 to RF-125). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val arrived = state.arrived
    LaunchedEffect(Unit) { viewModel.finishPendingSignIn() }
    LaunchedEffect(arrived) {
        if (arrived != null) {
            snackbar.showSnackbar(getPluralString(Res.plurals.sync_arrived, arrived, arrived))
            viewModel.arrivedShown()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.account_title), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            when (val account = state.account) {
                AccountState.SignedOut ->
                    item {
                        SignInPanel(
                            busy = state.signingIn,
                            problem = state.problem,
                            onSignIn = viewModel::signIn,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                is AccountState.SignedIn ->
                    item {
                        SignedInHeader(account.email, state.status, viewModel::syncNow, Modifier.padding(16.dp))
                    }
            }
            item { WhatTravels() }
            if (state.account is AccountState.SignedIn) {
                item {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedButton(onClick = viewModel::signOut) {
                            Text(stringResource(Res.string.account_sign_out), color = MaterialTheme.colorScheme.error)
                        }
                        Text(
                            stringResource(Res.string.account_sign_out_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/** "Leve sua estante junto" with the Google button; also the optional step of the first opening. */
@Composable
fun SignInPanel(
    busy: Boolean,
    problem: SignInProblem?,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (problem != null) ProblemCard(problem)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface), RoundedCornerShape(12.dp))
                .halftoneBehind(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComicTitle(stringResource(Res.string.account_pitch_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(Res.string.account_pitch), style = MaterialTheme.typography.bodyMedium)
            GoogleButton(
                stringResource(if (problem == SignInProblem.NOT_ALLOWED) Res.string.account_use_other else Res.string.account_sign_in),
                busy,
                onSignIn,
                Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(Res.string.account_offline_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProblemCard(problem: SignInProblem) {
    val (title, hint) =
        when (problem) {
            SignInProblem.NOT_ALLOWED -> Res.string.account_not_allowed to Res.string.account_not_allowed_hint
            SignInProblem.OFFLINE -> Res.string.account_sign_in_offline to null
            SignInProblem.FAILED -> Res.string.account_sign_in_failed to null
        }
    StatusCard(
        icon = if (problem == SignInProblem.NOT_ALLOWED) HqrIcons.Lock else HqrIcons.Warning,
        tint = MaterialTheme.colorScheme.error,
        title = stringResource(title),
        hint = hint?.let { stringResource(it) },
        alert = true,
    )
}

@Composable
private fun SignedInHeader(
    email: String,
    status: SyncStatus,
    onSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                ComicTitle(
                    email.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onPrimary),
                    maxLines = 1,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(email, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    stringResource(Res.string.account_google),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SyncStatusCard(status)
        when {
            status.running -> LinearProgressIndicator(Modifier.fillMaxWidth())
            status.error != null && status.error != SyncError.SESSION ->
                OutlinedButton(onClick = onSync) {
                    Icon(HqrIcons.Sync, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.sync_try_again))
                }
            else ->
                OutlinedButton(onClick = onSync, enabled = status.error == null) {
                    Icon(HqrIcons.Sync, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.sync_now))
                }
        }
    }
}

@Composable
private fun SyncStatusCard(status: SyncStatus) {
    val error = status.error
    when {
        status.running ->
            StatusCard(
                HqrIcons.Sync,
                MaterialTheme.colorScheme.primary,
                stringResource(Res.string.sync_running),
                stringResource(Res.string.sync_running_hint),
            )
        error != null -> {
            val hint =
                when (error) {
                    SyncError.OFFLINE -> Res.string.sync_failed_offline
                    SyncError.SERVER -> Res.string.sync_failed_server
                    SyncError.SESSION -> Res.string.sync_failed_session
                }
            StatusCard(
                HqrIcons.Warning,
                MaterialTheme.colorScheme.error,
                stringResource(Res.string.sync_failed),
                stringResource(hint),
                alert = true,
            )
        }
        else -> StatusCard(HqrIcons.Check, Color(0xFF2E7D32), syncedLabel(status.lastSuccessAt), null)
    }
}

@Composable
private fun syncedLabel(at: Long?): String {
    if (at == null) return stringResource(Res.string.sync_never)
    val ago = Clock.System.now().toEpochMilliseconds() - at
    return when {
        ago < MINUTE_MS -> stringResource(Res.string.sync_done_now)
        ago < HOUR_MS -> stringResource(Res.string.sync_done_minutes, (ago / MINUTE_MS).toInt())
        else -> stringResource(Res.string.sync_done_hours, (ago / HOUR_MS).toInt())
    }
}

@Composable
private fun StatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    hint: String?,
    alert: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.onSurface)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { if (alert) liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WhatTravels() {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Heading(Res.string.account_goes)
        Bullet(HqrIcons.Check, Res.string.account_goes_library)
        Bullet(HqrIcons.Check, Res.string.account_goes_progress)
        Bullet(HqrIcons.Check, Res.string.account_goes_feed)
        Heading(Res.string.account_stays)
        Bullet(HqrIcons.Phone, Res.string.account_stays_downloads, muted = true)
        Bullet(HqrIcons.Phone, Res.string.account_stays_device, muted = true)
    }
}

@Composable
private fun Heading(text: StringResource) {
    ComicTitle(stringResource(text), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun Bullet(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: StringResource,
    muted: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            null,
            Modifier.size(20.dp),
            tint = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
    }
}
