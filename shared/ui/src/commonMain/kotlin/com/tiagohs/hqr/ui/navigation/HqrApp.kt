package com.tiagohs.hqr.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.compose.setSingletonImageLoaderFactory
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.data.settings.ThemeMode
import com.tiagohs.hqr.data.sources.SourceConfig
import com.tiagohs.hqr.domain.repository.Connectivity
import com.tiagohs.hqr.domain.repository.Downloads
import com.tiagohs.hqr.domain.repository.Updates
import com.tiagohs.hqr.domain.source.ImageBytes
import com.tiagohs.hqr.domain.source.ServerManager
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.ui.components.DroppedFilesBanner
import com.tiagohs.hqr.ui.components.OfflineBanner
import com.tiagohs.hqr.ui.components.hqrImageLoader
import com.tiagohs.hqr.ui.downloads.DownloadsScreen
import com.tiagohs.hqr.ui.explore.BrowseSourceScreen
import com.tiagohs.hqr.ui.explore.ExploreScreen
import com.tiagohs.hqr.ui.explore.GlobalSearchScreen
import com.tiagohs.hqr.ui.history.HistoryScreen
import com.tiagohs.hqr.ui.library.CategoriesScreen
import com.tiagohs.hqr.ui.library.LibraryScreen
import com.tiagohs.hqr.ui.manga.MangaScreen
import com.tiagohs.hqr.ui.reader.ReaderScreen
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.error_offline_banner
import com.tiagohs.hqr.ui.resources.nav_explore
import com.tiagohs.hqr.ui.resources.nav_history
import com.tiagohs.hqr.ui.resources.nav_library
import com.tiagohs.hqr.ui.resources.nav_settings
import com.tiagohs.hqr.ui.settings.AboutScreen
import com.tiagohs.hqr.ui.settings.BackupScreen
import com.tiagohs.hqr.ui.settings.LocalAppLocale
import com.tiagohs.hqr.ui.settings.OnboardingScreen
import com.tiagohs.hqr.ui.settings.SettingsScreen
import com.tiagohs.hqr.ui.settings.UpdateNotice
import com.tiagohs.hqr.ui.settings.UpdateScreen
import com.tiagohs.hqr.ui.sources.AddServerScreen
import com.tiagohs.hqr.ui.sources.BridgeScreen
import com.tiagohs.hqr.ui.sources.ServerCatalogScreen
import com.tiagohs.hqr.ui.sources.SourceSettingsScreen
import com.tiagohs.hqr.ui.sources.SourcesScreen
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.HqrTheme
import com.tiagohs.hqr.ui.theme.LocalDynamicColor
import com.tiagohs.hqr.ui.theme.LocalPureBlack
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.getKoin
import org.koin.compose.koinInject

private data class TopLevel(
    val route: Route,
    val label: StringResource,
    val icon: ImageVector,
)

private val topLevels =
    listOf(
        TopLevel(Route.Library, Res.string.nav_library, HqrIcons.Library),
        TopLevel(Route.Explore, Res.string.nav_explore, HqrIcons.Explore),
        TopLevel(Route.History, Res.string.nav_history, HqrIcons.History),
        TopLevel(Route.Settings, Res.string.nav_settings, HqrIcons.Settings),
    )

/** The whole app: 4 destinations in a bar, rail or drawer by width (ADR-012), and the rest of the graph. */
@Composable
fun HqrApp(systemDark: Boolean) {
    val prefs = koinInject<AppPreferences>()
    val sourceConfig = koinInject<SourceConfig>()
    // RF-03, RF-105: the switches and the remote source list, once per start.
    val servers = koinInject<ServerManager>()
    val registry = koinInject<SourceRegistry>()
    LaunchedEffect(sourceConfig) {
        // RF-17, RF-88: the saved servers and switches first, from the device; screens wait for them.
        try {
            servers.start()
            sourceConfig.applySaved()
        } finally {
            registry.markReady()
        }
        // RF-105: the remote list may take a while; nothing waits for it.
        sourceConfig.refreshIfStale()
    }
    val downloads = getKoin().getOrNull<Downloads>()
    if (downloads != null) {
        // RF-63: a queue left with work (app closed, phone restarted) starts again.
        LaunchedEffect(downloads) { downloads.resumePending() }
    }
    val images = koinInject<ImageBytes>()
    setSingletonImageLoaderFactory { context -> hqrImageLoader(context, images) }
    val theme by prefs.theme.collectAsStateWithLifecycle()
    val dark =
        when (theme) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    // RF-87: a discreet banner while there is no network.
    val connectivity = getKoin().getOrNull<Connectivity>() ?: Connectivity.Always
    val online by connectivity.online.collectAsStateWithLifecycle()
    val language by prefs.appLanguage.collectAsStateWithLifecycle()
    val pureBlack by prefs.pureBlack.collectAsStateWithLifecycle()
    val onboarded by prefs.onboarded.collectAsStateWithLifecycle()
    val dynamicColor by prefs.dynamicColor.collectAsStateWithLifecycle()
    // RF-77: the chosen language for every screen; RF-76: pure black for every dark theme.
    CompositionLocalProvider(LocalAppLocale provides language, LocalPureBlack provides pureBlack, LocalDynamicColor provides dynamicColor) {
        key(language) {
            HqrTheme(dark = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!onboarded) {
                        // RF-81: once, before anything else.
                        OnboardingScreen(prefs.contentLanguages.value) { languages ->
                            prefs.setContentLanguages(languages)
                            prefs.setOnboarded()
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            if (!online) {
                                Box(
                                    Modifier.statusBarsPadding(),
                                ) { OfflineBanner(stringResource(Res.string.error_offline_banner)) }
                            }
                            DroppedFilesBanner()
                            Box(Modifier.weight(1f)) { AppNavigation(dark) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavigation(dark: Boolean) {
    val nav = rememberNavController()
    // RF-82: where the app updates itself, a new version is offered once a day at most.
    if (getKoin().getOrNull<Updates>() != null) UpdateNotice(onOpen = { nav.navigate(Route.Update) })
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val inReader = destination?.hasRoute(Route.Reader::class) == true
    val layout =
        if (inReader) {
            NavigationSuiteType.None
        } else {
            NavigationSuiteScaffoldDefaults.navigationSuiteType(
                androidx.compose.material3.adaptive
                    .currentWindowAdaptiveInfo(),
            )
        }
    NavigationSuiteScaffold(
        layoutType = layout,
        navigationSuiteItems = {
            topLevels.forEach { top ->
                val selected = destination?.hierarchy?.any { it.hasRoute(top.route::class) } == true
                item(
                    selected = selected,
                    onClick = { nav.navigateTopLevel(top.route) },
                    icon = { Icon(top.icon, contentDescription = null) },
                    label = { Text(stringResource(top.label)) },
                )
            }
        },
    ) {
        NavHost(nav, startDestination = Route.Library) {
            composable<Route.Library> {
                LibraryScreen(
                    onOpenManga = { nav.navigate(Route.Manga(it)) },
                    onContinue = { mangaId, chapterId, page -> nav.navigate(Route.Reader(mangaId, chapterId, page)) },
                    // The same as the bottom bar: Explore as a tab, never stacked over the Library (the Library tab must
                    // bring the Library back).
                    onExplore = { nav.navigateTopLevel(Route.Explore) },
                    onCategories = { nav.navigate(Route.Categories) },
                    onDownloads = { nav.navigate(Route.Downloads) },
                )
            }
            composable<Route.Downloads> {
                DownloadsScreen(onBack = { nav.popBackStack() }, onLibrary = { nav.navigateTopLevel(Route.Library) })
            }
            composable<Route.Categories> { CategoriesScreen(onBack = { nav.popBackStack() }) }
            composable<Route.Explore> {
                ExploreScreen(
                    onOpenSource = { nav.navigate(Route.BrowseSource(it)) },
                    onSearch = { nav.navigate(Route.Search) },
                    onManageSources = { nav.navigate(Route.Sources) },
                    onBridge = { nav.navigate(Route.Bridge) },
                    onOpenManga = { nav.navigate(Route.Manga(it)) },
                )
            }
            composable<Route.Search> {
                GlobalSearchScreen(
                    onBack = { nav.popBackStack() },
                    onOpenManga = { nav.navigate(Route.Manga(it)) },
                    onOpenSource = { id, query -> nav.navigate(Route.BrowseSource(id, query)) },
                    // RF-28: "Ver fontes" opens the sources screen.
                    onSources = { nav.navigate(Route.Sources) },
                )
            }
            composable<Route.Sources> {
                SourcesScreen(
                    onBack = { nav.popBackStack() },
                    onAddServer = { nav.navigate(Route.AddServer()) },
                    onEditServer = { nav.navigate(Route.AddServer(it)) },
                    onOpenServer = { nav.navigate(Route.ServerCatalog(it)) },
                    onBridge = { nav.navigate(Route.Bridge) },
                    onSourceSettings = { nav.navigate(Route.SourceSettings) },
                )
            }
            composable<Route.SourceSettings> { SourceSettingsScreen(onBack = { nav.popBackStack() }) }
            composable<Route.Bridge> { BridgeScreen(onBack = { nav.popBackStack() }) }
            composable<Route.AddServer> { back -> AddServerScreen(back.toRoute<Route.AddServer>().id, onDone = { nav.popBackStack() }) }
            composable<Route.ServerCatalog> { back ->
                ServerCatalogScreen(
                    sourceId = back.toRoute<Route.ServerCatalog>().sourceId,
                    onBack = { nav.popBackStack() },
                    onOpenManga = { nav.navigate(Route.Manga(it)) },
                )
            }
            composable<Route.History> {
                HistoryScreen(onResume = { mangaId, chapterId, page -> nav.navigate(Route.Reader(mangaId, chapterId, page)) })
            }
            composable<Route.Settings> {
                SettingsScreen(
                    onSources = { nav.navigate(Route.Sources) },
                    onDownloads = { nav.navigate(Route.Downloads) },
                    onBackup = { nav.navigate(Route.Backup) },
                    onAbout = { nav.navigate(Route.About) },
                    onUpdates = { nav.navigate(Route.Update) },
                )
            }
            composable<Route.About> { AboutScreen(onBack = { nav.popBackStack() }) }
            composable<Route.Update> { UpdateScreen(onBack = { nav.popBackStack() }) }
            composable<Route.Backup> { BackupScreen(onBack = { nav.popBackStack() }) }
            composable<Route.BrowseSource> { back ->
                val route = back.toRoute<Route.BrowseSource>()
                BrowseSourceScreen(
                    sourceId = route.sourceId,
                    initialQuery = route.query,
                    onBack = { nav.popBackStack() },
                    onOpenManga = { nav.navigate(Route.Manga(it)) },
                    onBridge = { nav.navigate(Route.Bridge) },
                )
            }
            composable<Route.Manga> { back ->
                MangaScreen(
                    mangaId = back.toRoute<Route.Manga>().id,
                    dark = dark,
                    onBack = { nav.popBackStack() },
                    onRead = { mangaId, chapterId, page -> nav.navigate(Route.Reader(mangaId, chapterId, page)) },
                )
            }
            composable<Route.Reader> { back ->
                val route = back.toRoute<Route.Reader>()
                ReaderScreen(route.mangaId, route.chapterId, route.page, onBack = { nav.popBackStack() })
            }
        }
        // RF-113: in the browser, each screen has its address and the back and forward buttons work.
        BrowserNavigation(nav)
    }
}

/** Top-level destinations swap like tabs, each keeping its own back stack (ADR-012). */
private fun NavHostController.navigateTopLevel(route: Route) =
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
