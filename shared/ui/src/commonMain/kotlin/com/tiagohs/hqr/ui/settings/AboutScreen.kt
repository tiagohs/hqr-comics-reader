package com.tiagohs.hqr.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.ui.di.AppVersion
import com.tiagohs.hqr.ui.di.CrashReports
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.about_based_on
import com.tiagohs.hqr.ui.resources.about_credit_hqnow
import com.tiagohs.hqr.ui.resources.about_credit_mangadex
import com.tiagohs.hqr.ui.resources.about_credits
import com.tiagohs.hqr.ui.resources.about_licenses
import com.tiagohs.hqr.ui.resources.about_no_content
import com.tiagohs.hqr.ui.resources.about_privacy
import com.tiagohs.hqr.ui.resources.about_privacy_link
import com.tiagohs.hqr.ui.resources.about_privacy_text
import com.tiagohs.hqr.ui.resources.about_source_code
import com.tiagohs.hqr.ui.resources.about_version
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.app_name
import com.tiagohs.hqr.ui.resources.settings_about
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.getKoin
import org.koin.compose.koinInject

/** The open-source libraries in the app and their licenses (RF-78). Names and licenses are not translated. */
private val LIBRARIES =
    listOf(
        "Kotlin, kotlinx.coroutines, kotlinx.serialization, kotlinx-datetime" to "Apache License 2.0",
        "Compose Multiplatform, Material 3, Navigation, Lifecycle" to "Apache License 2.0",
        "AndroidX Room, SQLite, WorkManager, Activity" to "Apache License 2.0",
        "Ktor" to "Apache License 2.0",
        "Koin" to "Apache License 2.0",
        "Coil" to "Apache License 2.0",
        "Okio" to "Apache License 2.0",
        "Telephoto (zoomable)" to "Apache License 2.0",
        "MaterialKolor" to "MIT License",
        "Multiplatform Settings" to "Apache License 2.0",
        "Firebase Crashlytics" to "Apache License 2.0",
        "Mihon, TachiyomiSY (NOTICE)" to "Apache License 2.0",
    )

private const val REPOSITORY = "https://github.com/tiagohs/hqr-comics-reader"
private const val PRIVACY = "https://tiagohs.github.io/hqr-comics-reader/politicy/"

/** SCR-SET-003: version, credits, licenses, privacy and the notice that HQR hosts no content (RF-78). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val version = koinInject<String>(AppVersion)
    val uri = LocalUriHandler.current
    // Crashlytics is listed only where the build has it.
    val libraries = if (getKoin().getOrNull<CrashReports>() != null) LIBRARIES else LIBRARIES.filterNot { it.first.startsWith("Firebase") }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.settings_about), style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
        )
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.titleLarge) },
                    supportingContent = { Text(stringResource(Res.string.about_version, version)) },
                )
                Paragraph(Res.string.about_no_content)
            }
            item { Section(Res.string.about_credits) }
            item {
                Paragraph(Res.string.about_credit_mangadex)
                Paragraph(Res.string.about_credit_hqnow)
                Paragraph(Res.string.about_based_on)
            }
            item { Section(Res.string.about_privacy) }
            item {
                Paragraph(Res.string.about_privacy_text)
                // RNF-29: the published policy, cited in the app.
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.about_privacy_link)) },
                    supportingContent = { Text(PRIVACY) },
                    modifier = Modifier.clickable { uri.openUri(PRIVACY) },
                )
            }
            item { Section(Res.string.about_licenses) }
            items(libraries) { (name, license) -> ListItem(headlineContent = { Text(name) }, supportingContent = { Text(license) }) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.about_source_code)) },
                    supportingContent = { Text(REPOSITORY) },
                    modifier = Modifier.clickable { uri.openUri(REPOSITORY) },
                )
            }
        }
    }
}

@Composable
private fun Section(title: StringResource) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp).semantics { heading() },
    )
}

@Composable
private fun Paragraph(text: StringResource) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}
