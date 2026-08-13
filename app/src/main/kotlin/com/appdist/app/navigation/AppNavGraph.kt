package com.appdist.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.appdist.feature.browse.ui.builds.BuildsScreen
import com.appdist.feature.browse.ui.projects.ProjectsScreen
import com.appdist.feature.builddetail.ui.BuildDetailScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "browse",
    ) {
        composable("browse") {
            ProjectsScreen(
                onProjectClick = { project ->
                    navController.navigate(
                        "browse/projects/${project.id}/builds" +
                            "?projectName=${Uri.encode(project.name)}" +
                            "&packageName=${Uri.encode(project.packageName)}"
                    )
                },
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
                onBack = { navController.popBackStack() },
                onBuildClick = { navController.navigate("builds/detail/$it") },
            )
        }
        composable(
            "builds/detail/{buildId}",
            arguments = listOf(navArgument("buildId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "appdist://builds/{buildId}" }),
        ) { backStack ->
            BuildDetailScreen(
                buildId = backStack.arguments?.getString("buildId") ?: "",
                onBack = { navController.popBackStack() },
            )
        }
    }
}
