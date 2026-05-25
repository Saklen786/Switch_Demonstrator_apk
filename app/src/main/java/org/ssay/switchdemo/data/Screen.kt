package org.ssay.switchdemo.data

/**
 * FIXED #35: replaces the previous String-based currentScreen.
 * A typo on the navigation call site is now a compile error instead of
 * a silent blank screen at runtime.
 */
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Settings  : Screen("settings")
    data object About     : Screen("about")

    companion object {
        val all: List<Screen> = listOf(Dashboard, Settings, About)
        fun fromRoute(route: String): Screen = all.firstOrNull { it.route == route } ?: Dashboard
    }
}
