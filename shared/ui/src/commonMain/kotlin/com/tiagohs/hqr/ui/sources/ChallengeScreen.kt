package com.tiagohs.hqr.ui.sources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.challenge_hint
import com.tiagohs.hqr.ui.resources.challenge_title
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.stringResource

/**
 * The source's page in a browser inside the app, where Cloudflare checks the user (RF-106). Where there is no such
 * browser (the web app), [ChallengeBrowser] says so.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    url: String,
    onPassed: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.challenge_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                stringResource(Res.string.challenge_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ChallengeBrowser(url, onPassed, Modifier.fillMaxSize())
        }
    }
}

/** The page, watched until the clearance cookie shows up; then [onPassed]. */
@Composable
expect fun ChallengeBrowser(
    url: String,
    onPassed: () -> Unit,
    modifier: Modifier = Modifier,
)
