package com.example.farmermilkcollectionapp.ui

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.farmermilkcollectionapp.R
import kotlinx.coroutines.delay

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
// Splash Screen
// ----------------------------------------------------------------
@Composable
fun SplashScreen(navController: NavHostController) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(true) {
        scale.animateTo(
            targetValue = 0.8f,
            animationSpec = tween(
                durationMillis = 900,
                easing = { OvershootInterpolator(4f).getInterpolation(it) }
            )
        )
        delay(1500)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_farmer),
            contentDescription = "App Logo",
            modifier = Modifier.scale(scale.value)
        )
    }
}

// ----------------------------------------------------------------
// SIMPLE PLACEHOLDERS (You already have MilkScreen.kt)
// Replace History & Settings later.
// ----------------------------------------------------------------

