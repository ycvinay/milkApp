package com.example.farmermilkcollectionapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

// --- 1. IMPORT YOUR EXTERNAL SPLASH SCREEN ---
// (This import tells the file to use your green gradient splash screen)
import com.example.farmermilkcollectionapp.ui.SplashScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    // Determine if bottom bar should show
    val navBackStack by navController.currentBackStackEntryAsState()
    val route = navBackStack?.destination?.route
    val showBottomBar = route in listOf("home", "history", "settings")

    Scaffold(
        bottomBar = {
            if (showBottomBar) BottomNavBar(navController)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(padding)
        ) {
            // This will now call your external SplashScreen.kt file
            composable("splash") { SplashScreen(navController) }

            composable("home") { MilkScreen() }          // ⭐ Home = MilkScreen
            composable("history") { HistoryScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

// ----------------------------------------------------------------
// Bottom Navigation Bar
// ----------------------------------------------------------------
@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("home", "Home", Icons.Default.Home),
        BottomNavItem("history", "History", Icons.Default.History),
        BottomNavItem("settings", "Settings", Icons.Default.Settings)
    )

    NavigationBar {
        val navBackStack by navController.currentBackStackEntryAsState()
        val currentDest = navBackStack?.destination

        items.forEach { item ->
            NavigationBarItem(
                selected = currentDest?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

data class BottomNavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

// ----------------------------------------------------------------
// Splash Screen (DELETED)
// ----------------------------------------------------------------
// The simple SplashScreen function that was here has been DELETED.
// The NavHost now calls your external SplashScreen.kt file via the import.
// ----------------------------------------------------------------
