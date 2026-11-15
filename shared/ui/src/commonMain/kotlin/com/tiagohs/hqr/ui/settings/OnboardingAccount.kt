package com.tiagohs.hqr.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.repository.AccountState
import com.tiagohs.hqr.presentation.account.AccountViewModel
import com.tiagohs.hqr.ui.components.ComicMotion
import com.tiagohs.hqr.ui.components.halftoneBehind
import com.tiagohs.hqr.ui.components.rememberEntrance
import com.tiagohs.hqr.ui.components.sketchFrame
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.account_pitch_title
import com.tiagohs.hqr.ui.resources.account_sign_in
import com.tiagohs.hqr.ui.resources.onboarding_account_body
import com.tiagohs.hqr.ui.resources.onboarding_account_hint
import com.tiagohs.hqr.ui.resources.onboarding_not_now
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.bangers
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** SCR-ONB-001, optional account step (RF-119): right after the first opening, only where signing in exists. */
@Composable
fun OnboardingAccountStep(
    onDone: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signedIn = state.account is AccountState.SignedIn
    LaunchedEffect(signedIn, viewModel.available) { if (signedIn || !viewModel.available) onDone() }
    if (!viewModel.available) return
    val ink = Color(0xFF16161A)
    val paper = MaterialTheme.colorScheme.background
    val drawn = rememberEntrance(ComicMotion.SKETCH_MS)
    Surface(Modifier.fillMaxSize(), color = paper) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to ink, 0.3f to ink.copy(alpha = 0.6f), 0.48f to paper))
                .halftoneBehind(ink.copy(alpha = 0.25f), step = 6.dp, radius = 1.4.dp, until = 0.42f)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(Res.string.account_pitch_title),
                fontFamily = bangers(),
                fontSize = 40.sp,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFFF6F1E7),
                style = TextStyle(shadow = Shadow(MaterialTheme.colorScheme.primary, Offset(3f, 3f), 0f)),
                modifier = Modifier.padding(top = 48.dp).semantics { heading() },
            )
            Text(
                stringResource(Res.string.onboarding_account_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFF6F1E7),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Column(
                Modifier
                    .padding(top = 28.dp)
                    .widthIn(max = 320.dp)
                    .rotate(-1.5f)
                    .background(Color(0xFFFFFDF7))
                    .sketchFrame(ink, 2.dp) { drawn.value }
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(HqrIcons.Phone, null, tint = ink, modifier = Modifier.size(40.dp))
                    Icon(HqrIcons.Sync, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Icon(HqrIcons.Library, null, tint = ink, modifier = Modifier.size(40.dp))
                }
                Text(
                    stringResource(Res.string.onboarding_account_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink,
                    textAlign = TextAlign.Center,
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
            ) {
                GoogleButton(stringResource(Res.string.account_sign_in), state.signingIn, viewModel::signIn, Modifier.fillMaxWidth())
                OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(stringResource(Res.string.onboarding_not_now))
                }
            }
        }
    }
}
