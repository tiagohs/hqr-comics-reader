package com.tiagohs.hqr.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.tiagohs.hqr.domain.source.ImageBytes
import com.tiagohs.hqr.ui.components.hqrImageLoader
import com.tiagohs.hqr.ui.explore.BrowseSourceScreen
import com.tiagohs.hqr.ui.explore.ExploreScreen
import com.tiagohs.hqr.ui.history.HistoryScreen
import com.tiagohs.hqr.ui.library.CategoriesScreen
import com.tiagohs.hqr.ui.library.LibraryScreen
import com.tiagohs.hqr.ui.manga.MangaScreen
import com.tiagohs.hqr.ui.reader.ReaderScreen
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.nav_explore
import com.tiagohs.hqr.ui.resources.nav_history
import com.tiagohs.hqr.ui.resources.nav_library
import com.tiagohs.hqr.ui.resources.nav_settings
import com.tiagohs.hqr.ui.settings.SettingsScreen
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.HqrTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
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
    val images = koinInject<ImageBytes>()
    setSingletonImageLoaderFactory { context -> hqrImageLoader(context, images) }
    val theme by prefs.theme.collectAsStateWithLifecycle()
    val dark =
        when (theme) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    HqrTheme(dark = dark) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppNavigation(dark)
        }
    }
}

@Composable
private fun AppNavigation(dark: Boolean) {
    val nav = rememberNavController()
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
                )
            }
            composable<Route.Categories> { CategoriesScreen(onBack = { nav.popBackStack() }) }
            composable<Route.Explore> { ExploreScreen(onOpenSource = { nav.navigate(Route.BrowseSource(it)) }) }
            composable<Route.History> {
                HistoryScreen(onResume = { mangaId, chapterId, page -> nav.navigate(Route.Reader(mangaId, chapterId, page)) })
            }
            composable<Route.Settings> { SettingsScreen() }
            composable<Route.BrowseSource> { back ->
                BrowseSourceScreen(
                    sourceId = back.toRoute<Route.BrowseSource>().sourceId,
                    onBack = { nav.popBackStack() },
                    onOpenManga = { nav.navigate(Route.Manga(it)) },
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
    }
}

/** Top-level destinations swap like tabs, each keeping its own back stack (ADR-012). */
private fun NavHostController.navigateTopLevel(route: Route) =
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
