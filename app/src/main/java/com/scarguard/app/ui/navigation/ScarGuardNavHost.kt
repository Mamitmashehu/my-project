package com.scarguard.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scarguard.app.ui.baseline.BaselineScreen
import com.scarguard.app.ui.connect.ConnectScreen
import com.scarguard.app.ui.history.HistoryScreen
import com.scarguard.app.ui.home.HomeScreen
import com.scarguard.app.ui.monitor.MonitorScreen
import com.scarguard.app.ui.settings.SettingsScreen

private data class BottomBarItem(val destination: Destination, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomBarItem(Destination.Home, "Home", Icons.Filled.Home),
    BottomBarItem(Destination.Connect, "Device", Icons.Filled.Bluetooth),
    BottomBarItem(Destination.History, "History", Icons.Filled.History),
    BottomBarItem(Destination.Settings, "Settings", Icons.Filled.Settings),
)

// Peer tabs (Home/Device/History/Settings) cross-fade into each other -- there's no
// "forward/back" relationship between them, so a slide would read as arbitrary.
private val tabTransitionSpec = tween<Float>(220)
private val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { fadeIn(tabTransitionSpec) }
private val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { fadeOut(tween(160)) }

// Baseline/Monitor are full-screen flows pushed on top of Home, so they get a modal-style
// slide up on the way in and slide down on the way out -- a clear "this is a task" feel.
private val modalEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(320)) + fadeIn(tween(320))
}
private val modalExitUnderneath: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { fadeOut(tween(150)) }
private val modalPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { fadeIn(tween(200)) }
private val modalPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(280)) + fadeOut(tween(280))
}

@Composable
fun ScarGuardNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    val showBottomBar = bottomItems.any { currentRoute?.hierarchy?.any { dest -> dest.route == it.destination.route } == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp,
                    ) {
                        bottomItems.forEach { item ->
                            val selected = currentRoute?.hierarchy?.any { it.route == item.destination.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding),
            enterTransition = tabEnter,
            exitTransition = tabExit,
            popEnterTransition = tabEnter,
            popExitTransition = tabExit,
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onGoToConnect = { navController.navigate(Destination.Connect.route) },
                    onSetBaseline = { navController.navigate(Destination.Baseline.route) },
                    onTakeFollowUp = { navController.navigate(Destination.Monitor.route) },
                )
            }
            composable(Destination.Connect.route) { ConnectScreen() }
            composable(Destination.History.route) { HistoryScreen() }
            composable(Destination.Settings.route) { SettingsScreen() }
            composable(
                Destination.Baseline.route,
                enterTransition = modalEnter,
                exitTransition = modalExitUnderneath,
                popEnterTransition = modalPopEnter,
                popExitTransition = modalPopExit,
            ) {
                BaselineScreen(
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(
                Destination.Monitor.route,
                enterTransition = modalEnter,
                exitTransition = modalExitUnderneath,
                popEnterTransition = modalPopEnter,
                popExitTransition = modalPopExit,
            ) {
                MonitorScreen(
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
        }
    }
}
