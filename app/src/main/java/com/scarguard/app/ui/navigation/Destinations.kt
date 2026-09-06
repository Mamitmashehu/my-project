package com.scarguard.app.ui.navigation

sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Connect : Destination("connect")
    data object Baseline : Destination("baseline")
    data object Monitor : Destination("monitor")
    data object History : Destination("history")
    data object Settings : Destination("settings")
}

val bottomBarDestinations = listOf(
    Destination.Home,
    Destination.Connect,
    Destination.History,
    Destination.Settings,
)
