package com.fitlife.ai.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.fitlife.ai.ui.screens.auth.LoginScreen
import com.fitlife.ai.ui.screens.auth.SignupScreen
import com.fitlife.ai.ui.screens.auth.viewmodel.AuthViewModel
import com.fitlife.ai.ui.screens.onboarding.OnboardingScreen
import com.fitlife.ai.ui.screens.home.HomeScreen
import com.fitlife.ai.ui.screens.workout.WorkoutScreen
import com.fitlife.ai.ui.screens.workout.ActiveSessionScreen
import com.fitlife.ai.ui.screens.nutrition.NutritionScreen
import com.fitlife.ai.ui.screens.progress.ProgressScreen
import com.fitlife.ai.ui.screens.profile.ProfileScreen
import com.fitlife.ai.ui.screens.ai.ChatScreen
import com.fitlife.ai.ui.screens.ai.CoachScreen
import com.fitlife.ai.ui.screens.blood.BloodAnalysisScreen
import com.fitlife.ai.ui.screens.cycle.CycleTrackingScreen
import com.fitlife.ai.ui.screens.settings.SettingsScreen
import com.fitlife.ai.ui.theme.FitLifeBottomBar

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HOME = "home"
    const val WORKOUTS = "workouts"
    const val WORKOUT_DETAIL = "workouts/{programId}"
    const val ACTIVE_SESSION = "workouts/session/{programId}"
    const val NUTRITION = "nutrition"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val AI_CHAT = "ai/chat"
    const val AI_COACH = "ai/coach"
    const val BLOOD = "blood"
    const val BLOOD_DETAIL = "blood/{analysisId}"
    const val CYCLE = "cycle"
    const val SETTINGS = "settings"
    const val ONBOARDING_STEP = "onboarding/{step}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitLifeNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val startDestination = when {
        !authState.isLoggedIn -> Routes.ONBOARDING
        !authState.onboardingCompleted -> Routes.ONBOARDING
        else -> Routes.HOME
    }

    val bottomBarRoutes = listOf("home", "workouts", "nutrition", "progress", "profile")
    val showBottomBar = currentRoute?.let { route ->
        bottomBarRoutes.any { route.startsWith(it) }
    } ?: false

    LaunchedEffect(authState.isLoggedIn, authState.onboardingCompleted) {
        if (authState.isLoggedIn && authState.onboardingCompleted) {
            navController.navigate(Routes.HOME) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                FitLifeBottomBar(
                    selectedRoute = currentRoute ?: "home",
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onSignupClick = { navController.navigate(Routes.SIGNUP) },
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SIGNUP) {
                SignupScreen(
                    authViewModel = authViewModel,
                    onLoginClick = { navController.popBackStack() },
                    onSignupSuccess = {
                        navController.navigate(Routes.ONBOARDING_STEP, arguments = listOf(navArgument("step") { type = NavType.IntType; defaultValue = 1 })) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                Routes.ONBOARDING_STEP,
                arguments = listOf(navArgument("step") { type = NavType.IntType })
            ) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToChat = { navController.navigate(Routes.AI_CHAT) },
                    onNavigateToWorkouts = { navController.navigate(Routes.WORKOUTS) },
                    onNavigateToNutrition = { navController.navigate(Routes.NUTRITION) }
                )
            }

            composable(Routes.WORKOUTS) {
                WorkoutScreen(
                    onProgramClick = { programId ->
                        navController.navigate("workouts/$programId")
                    },
                    onStartSession = { programId ->
                        navController.navigate("workouts/session/$programId")
                    }
                )
            }

            composable(
                Routes.WORKOUT_DETAIL,
                arguments = listOf(navArgument("programId") { type = NavType.StringType })
            ) { backStackEntry ->
                val programId = backStackEntry.arguments?.getString("programId") ?: return@composable
                ActiveSessionScreen(
                    programId = programId,
                    onFinish = { navController.popBackStack() }
                )
            }

            composable(
                Routes.ACTIVE_SESSION,
                arguments = listOf(navArgument("programId") { type = NavType.StringType })
            ) { backStackEntry ->
                val programId = backStackEntry.arguments?.getString("programId") ?: return@composable
                ActiveSessionScreen(
                    programId = programId,
                    onFinish = { navController.popBackStack() }
                )
            }

            composable(Routes.NUTRITION) {
                NutritionScreen()
            }

            composable(Routes.PROGRESS) {
                ProgressScreen()
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToBlood = { navController.navigate(Routes.BLOOD) },
                    onNavigateToCycle = { navController.navigate(Routes.CYCLE) }
                )
            }

            composable(Routes.AI_CHAT) {
                ChatScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.AI_COACH) {
                CoachScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.BLOOD) {
                BloodAnalysisScreen(onBack = { navController.popBackStack() })
            }

            composable(
                Routes.BLOOD_DETAIL,
                arguments = listOf(navArgument("analysisId") { type = NavType.StringType })
            ) {
                BloodAnalysisScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.CYCLE) {
                CycleTrackingScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
