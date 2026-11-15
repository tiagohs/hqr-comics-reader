package com.tiagohs.hqr.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.account_see
import com.tiagohs.hqr.ui.resources.sync_stale_days
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Google's sign-in button look: white, thin grey border, the colored "G". */
@Composable
fun GoogleButton(
    label: String,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !busy,
        modifier = modifier.height(48.dp),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1F1F1F),
                disabledContainerColor = Color.White,
                disabledContentColor = Color(0xFF1F1F1F).copy(alpha = 0.6f),
            ),
        border = BorderStroke(1.dp, Color(0xFF747775)),
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF1F1F1F))
        } else {
            Icon(HqrIcons.GoogleMark, null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

/** The text of the library's notice: "Não sincroniza há 2 dias" (RF-125). */
@Composable
fun SyncStaleNotice(
    days: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = pluralStringResource(Res.plurals.sync_stale_days, days, days)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF1A8))
            .border(1.dp, Color(0x4D16161A), RoundedCornerShape(12.dp))
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(HqrIcons.CloudOff, null, tint = Color(0xFF16161A), modifier = Modifier.size(20.dp))
        Text(
            label,
            Modifier.weight(1f),
            color = Color(0xFF16161A),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = onOpen) {
            Text(stringResource(Res.string.account_see), color = Color(0xFF16161A), fontWeight = FontWeight.Bold)
        }
    }
}
