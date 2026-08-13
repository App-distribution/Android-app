package com.appdist.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : BottomNavDestination("home", "Главная", Icons.Default.Home)
    data object Browse : BottomNavDestination("browse", "Сборки", Icons.Default.Search)
    data object Mine : BottomNavDestination("mine", "Мои", Icons.Default.DownloadDone)
    data object Profile : BottomNavDestination("profile", "Профиль", Icons.Default.Person)
}

/**
 * Только те вкладки, для которых в AppNavGraph есть маршрут. Home и Profile
 * пока не подключены — их показ привёл бы к падению при нажатии, потому что
 * navigate() по несуществующему маршруту бросает IllegalArgumentException.
 */
val bottomNavItems = listOf(
    BottomNavDestination.Browse,
    BottomNavDestination.Mine
)

@Composable
fun BottomNavBar(navController: NavController) {
    val backStack = navController.currentBackStackEntryAsState()
    val currentRoute = backStack.value?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { dest ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = {
                    navController.navigate(dest.route) {
                        // Стартовый пункт графа, а не жёстко заданный "home":
                        // такого маршрута в графе нет, и popUpTo по нему падает.
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}
