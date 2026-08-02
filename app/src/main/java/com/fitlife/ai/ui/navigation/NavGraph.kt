package com.fitlife.ai.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fitlife.ai.ui.screens.ai.AIChatScreen
import com.fitlife.ai.ui.screens.auth.LoginScreen
import com.fitlife.ai.ui.screens.auth.SignupScreen
import com.fitlife.ai.ui.screens.blood.BloodScreen
import com.fitlife.ai.ui.screens.camera.CalorieTrackerScreen
import com.fitlife.ai.ui.screens.cycle.CycleScreen
import com.fitlife.ai.ui.screens.home.HomeScreen
import com.fitlife.ai.ui.screens.onboarding.OnboardingScreen
import com.fitlife.ai.ui.screens.profile.ProfileScreen
import com.fitlife.ai.ui.screens.settings.SettingsScreen
import com.fitlife.ai.ui.screens.workout.WorkoutScreen
import com.fitlife.ai.viewmodel.AuthViewModel

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Default.Home, Routes.Home.route),
    BottomNavItem("Workout", Icons.Default.FitnessCenter, Routes.Workout.route),
    BottomNavItem("AI Chat", Icons.Default.SmartToy, Routes.AIChat.route),
    BottomNavItem("Calories", Icons.Default.DirectionsRun, Routes.CalorieTracker.route),
    BottomNavItem("Profile", Icons.Default.Person, Routes.Profile.route),
)

val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val sessionChecked by authViewModel.sessionChecked.collectAsState()

    if (!sessionChecked) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(isLoggedIn) {
        val current = currentDestination?.route
        if (!isLoggedIn && current != null && current != Routes.Login.route && current != Routes.Signup.route) {
            navController.navigate(Routes.Login.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            }
        }
    }
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Routes.Onboarding.route else Routes.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Login.route) {
                LoginScreen(
                    onNavigateToSignup = { navController.navigate(Routes.Signup.route) },
                    onLoginSuccess = {
                        navController.navigate(Routes.Onboarding.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    },
                    viewModel = authViewModel
                )
            }
            composable(Routes.Signup.route) {
                SignupScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onSignupSuccess = {
                        navController.navigate(Routes.Onboarding.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    },
                    viewModel = authViewModel
                )
            }
            composable(Routes.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.Home.route) {
                HomeScreen(
                    onOpenBlood = { navController.navigate(Routes.Blood.route) },
                    onOpenCycle = { navController.navigate(Routes.Cycle.route) }
                )
            }
            composable(Routes.Profile.route) { ProfileScreen(onNavigateToSettings = { navController.navigate(Routes.Settings.route) }) }
            composable(Routes.Workout.route) { WorkoutScreen() }
            composable(Routes.AIChat.route) { AIChatScreen() }
            composable(Routes.CalorieTracker.route) { CalorieTrackerScreen() }
            composable(Routes.Blood.route) { BloodScreen() }
            composable(Routes.Cycle.route) { CycleScreen() }
            composable(Routes.Settings.route) { SettingsScreen(viewModel = authViewModel) }
        }
    }
}
