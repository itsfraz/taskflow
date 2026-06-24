package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.MyApplication
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

@Composable
fun TaskFlowApp() {
    val navController = rememberNavController()
    
    // Resolve ViewModel through Factory
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext as MyApplication
    val taskViewModel: TaskViewModel = viewModel(
        factory = TaskViewModelFactory(context.repository)
    )

    // Collect settings from dataStore preferences
    val currentTheme by taskViewModel.themeState.collectAsState()
    val fontScaleMultiplier by taskViewModel.fontSizeScale.collectAsState()

    val darkTheme = when (currentTheme) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }

    MyApplicationTheme(darkTheme = darkTheme) {
        // Dynamic Font Scaling Injector using Density Provider wrapper!
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = LocalDensity.current.density,
                fontScale = LocalDensity.current.fontScale * fontScaleMultiplier
            )
        ) {
            NavHost(
                navController = navController, 
                startDestination = "splash",
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                composable("splash") {
                    SplashScreen(
                        onNavigateToDashboard = {
                            navController.navigate("dashboard") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    "dashboard",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(250))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(250))
                    }
                ) {
                    DashboardScreen(
                        viewModel = taskViewModel,
                        onNavigateToDetail = { taskId ->
                            navController.navigate("detail/$taskId")
                        },
                        onNavigateToCalendar = {
                            navController.navigate("calendar")
                        },
                        onNavigateToStats = {
                            navController.navigate("stats")
                        },
                        onNavigateToSettings = {
                            navController.navigate("settings")
                        },
                        onNavigateToFocus = {
                            navController.navigate("focus")
                        }
                    )
                }

                composable(
                    route = "detail/{taskId}",
                    arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
                    enterTransition = {
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(450, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(250))
                    },
                    popExitTransition = {
                        slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(450, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                ) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
                    TaskDetailScreen(
                        taskId = taskId,
                        viewModel = taskViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    "calendar",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(250))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(250))
                    }
                ) {
                    CalendarScreen(
                        viewModel = taskViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToDetail = { taskId ->
                            navController.navigate("detail/$taskId")
                        }
                    )
                }

                composable(
                    "stats",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(250))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(250))
                    }
                ) {
                    StatisticsScreen(
                        viewModel = taskViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    "settings",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(250))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(250))
                    }
                ) {
                    SettingsScreen(
                        viewModel = taskViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    "focus",
                    enterTransition = {
                        slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                ) {
                    FocusModeScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
