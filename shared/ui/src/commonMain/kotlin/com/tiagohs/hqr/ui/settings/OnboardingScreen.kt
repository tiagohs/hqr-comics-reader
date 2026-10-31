package com.tiagohs.hqr.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiagohs.hqr.ui.components.halftoneBehind
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.onboarding_no_content
import com.tiagohs.hqr.ui.resources.onboarding_paged
import com.tiagohs.hqr.ui.resources.onboarding_paged_tag
import com.tiagohs.hqr.ui.resources.onboarding_reading_hint
import com.tiagohs.hqr.ui.resources.onboarding_reading_question
import com.tiagohs.hqr.ui.resources.onboarding_select_language
import com.tiagohs.hqr.ui.resources.onboarding_start
import com.tiagohs.hqr.ui.resources.onboarding_webtoon
import com.tiagohs.hqr.ui.resources.onboarding_webtoon_tag
import com.tiagohs.hqr.ui.theme.bangers
import org.jetbrains.compose.resources.stringResource

/**
 * SCR-ONB-001 (RF-81): how the person likes to read, the content languages and the notice that HQR hosts nothing, once,
 * before anything else. The page is drawn like a comic splash: ink at the top fading into halftone and paper.
 */
@Composable
fun OnboardingScreen(
    initial: Set<String>,
    onStart: (languages: Set<String>, webtoon: Boolean) -> Unit,
) {
    var languages by rememberSaveable { mutableStateOf(initial.toList()) }
    var webtoon by rememberSaveable { mutableStateOf(false) }
    val ink = Color(0xFF16161A)
    val paper = MaterialTheme.colorScheme.background
    Surface(Modifier.fillMaxSize(), color = paper) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to ink, 0.3f to ink.copy(alpha = 0.6f), 0.48f to paper))
                .halftoneBehind(ink.copy(alpha = 0.25f), step = 6.dp, radius = 1.4.dp, until = 0.42f)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.onboarding_reading_question),
                    fontFamily = bangers(),
                    fontSize = 40.sp,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFFF6F1E7),
                    style = TextStyle(shadow = Shadow(MaterialTheme.colorScheme.primary, Offset(3f, 3f), 0f)),
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(Res.string.onboarding_reading_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF6F1E7),
                    textAlign = TextAlign.Center,
                )
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ReadingCard(
                        stringResource(Res.string.onboarding_paged),
                        stringResource(Res.string.onboarding_paged_tag),
                        selected = !webtoon,
                        tilt = -2f,
                    ) { webtoon = false }
                    ReadingCard(
                        stringResource(Res.string.onboarding_webtoon),
                        stringResource(Res.string.onboarding_webtoon_tag),
                        selected = webtoon,
                        tilt = 1.5f,
                    ) { webtoon = true }
                }
                Text(stringResource(Res.string.onboarding_select_language), fontFamily = bangers(), fontSize = 20.sp)
                ContentLanguageChips(languages.toSet(), { languages = it.toList() })
                Text(
                    stringResource(Res.string.onboarding_no_content),
                    style = MaterialTheme.typography.bodySmall,
                    color = ink,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .rotate(-1f)
                            .background(Color(0xFFFFF1A8))
                            .border(2.dp, ink)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Button(
                onClick = { onStart(languages.toSet(), webtoon) },
                modifier =
                    Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
                        .height(56.dp),
            ) { Text(stringResource(Res.string.onboarding_start), style = MaterialTheme.typography.titleMedium) }
        }
    }
}

/** One reading mode as a hand-drawn card with its tag; a radio button for assistive technology. */
@Composable
private fun ReadingCard(
    title: String,
    tag: String,
    selected: Boolean,
    tilt: Float,
    onClick: () -> Unit,
) {
    val ink = Color(0xFF16161A)
    Box(Modifier.rotate(tilt)) {
        Column(
            Modifier
                .width(220.dp)
                .drawBehind { drawRect(ink, topLeft = Offset(5.dp.toPx(), 5.dp.toPx()), size = size) }
                .background(Color(0xFFFFFDF7))
                .border(if (selected) 4.dp else 2.dp, if (selected) MaterialTheme.colorScheme.primary else ink)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, fontFamily = bangers(), fontSize = 24.sp, lineHeight = 24.sp, color = ink, textAlign = TextAlign.Center)
        }
        Text(
            tag,
            fontFamily = bangers(),
            fontSize = 15.sp,
            color = Color.White,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .rotate(6f)
                    .background(if (selected) MaterialTheme.colorScheme.primary else ink)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
