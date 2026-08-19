package com.appdist.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.appdist.core.ui.components.ActionZoneSide
import com.appdist.feature.browse.ui.builds.BuildsScreen
import com.appdist.feature.browse.ui.projects.ProjectsScreen
import com.appdist.feature.builddetail.ui.BuildDetailScreen
import com.appdist.feature.mine.ui.MineScreen
import com.appdist.feature.settings.ui.PermissionsScreen
import com.appdist.feature.settings.ui.SettingsScreen

private const val BrowseRoute = "browse"
private const val DownloadsRoute = "mine"
private const val SettingsRoute = "settings"

@Composable
fun AppNavGraph(viewModel: AppNavViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val actionZoneOnLeft by viewModel.actionZoneOnLeft.collectAsStateWithLifecycle()
    val actionZoneSide = if (actionZoneOnLeft) ActionZoneSide.Left else ActionZoneSide.Right

    NavHost(
        navController = navController,
        startDestination = BrowseRoute,
    ) {
        composable(BrowseRoute) {
            ProjectsScreen(
                actionZoneSide = actionZoneSide,
                onProjectClick = { project ->
                    navController.navigate(
                        "browse/projects/${project.id}/builds" +
                            "?projectName=${Uri.encode(project.name)}" +
                            "&packageName=${Uri.encode(project.packageName)}"
                    )
                },
                onDownloadsClick = { navController.navigateTopLevel(DownloadsRoute) },
                onSettingsClick = { navController.navigate(SettingsRoute) },
            )
        }
        composable(
            "browse/projects/{projectId}/builds?projectName={projectName}&packageName={packageName}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("projectName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("packageName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStack ->
            BuildsScreen(
                projectId = backStack.arguments?.getString("projectId") ?: "",
                projectName = backStack.arguments?.getString("projectName") ?: "",
                packageName = backStack.arguments?.getString("packageName") ?: "",
                actionZoneSide = actionZoneSide,
                onBack = { navController.popBackStack() },
                onBuildClick = { navController.navigate("builds/detail/$it") },
                onDownloadsClick = { navController.navigateTopLevel(DownloadsRoute) },
                onSettingsClick = { navController.navigate(SettingsRoute) },
            )
        }
        composable(
            "builds/detail/{buildId}",
            arguments = listOf(navArgument("buildId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "appdist://builds/{buildId}" }),
        ) { backStack ->
            BuildDetailScreen(
                buildId = backStack.arguments?.getString("buildId") ?: "",
                actionZoneSide = actionZoneSide,
                onBack = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(SettingsRoute) },
            )
        }
        composable(DownloadsRoute) {
            MineScreen(
                actionZoneSide = actionZoneSide,
                onBuildClick = { navController.navigate("builds/detail/$it") },
                onJournalsClick = { navController.navigateTopLevel(BrowseRoute) },
                onSettingsClick = { navController.navigate(SettingsRoute) },
            )
        }
        composable(SettingsRoute) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onPermissionsClick = { navController.navigate("settings/permissions") },
            )
        }
        composable("settings/permissions") {
            PermissionsScreen(
                actionZoneSide = actionZoneSide,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
