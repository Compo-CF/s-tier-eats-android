package com.compofelice.stiereats.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.compofelice.stiereats.ui.screens.AdminScreen
import com.compofelice.stiereats.ui.screens.BrowseScreen
import com.compofelice.stiereats.ui.screens.CommunityScreen
import com.compofelice.stiereats.ui.screens.DetailScreen
import com.compofelice.stiereats.ui.screens.MapScreen
import com.compofelice.stiereats.ui.screens.MyTiersScreen
import com.compofelice.stiereats.ui.screens.ProfileScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("map", "Map", Icons.Filled.Map),
    Tab("browse", "Browse", Icons.Filled.Restaurant),
    Tab("mytiers", "My Tiers", Icons.Filled.Star),
    Tab("community", "Community", Icons.Filled.Groups),
    Tab("profile", "Profile", Icons.Filled.Person),
)

@Composable
fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val dest = backStack?.destination
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = dest?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "map",
            modifier = Modifier.padding(padding),
        ) {
            val open: (String) -> Unit = { id -> nav.navigate("detail/$id") }
            composable("map") { MapScreen(vm, open) }
            composable("browse") { BrowseScreen(vm, open) }
            composable("mytiers") { MyTiersScreen(vm, open) }
            composable("community") { CommunityScreen(vm, open) }
            composable("profile") { ProfileScreen(vm, onOpenAdmin = { nav.navigate("admin") }) }
            composable("admin") { AdminScreen(vm, onBack = { nav.popBackStack() }) }
            composable("detail/{id}") { entry ->
                DetailScreen(
                    restaurantId = entry.arguments?.getString("id") ?: "",
                    vm = vm,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
