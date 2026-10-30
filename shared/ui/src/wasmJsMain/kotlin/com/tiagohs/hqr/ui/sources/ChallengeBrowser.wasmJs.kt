package com.tiagohs.hqr.ui.sources

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.challenge_unavailable
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun ChallengeBrowser(
    url: String,
    onPassed: () -> Unit,
    modifier: Modifier,
) {
    Text(stringResource(Res.string.challenge_unavailable), modifier.padding(16.dp))
}
