package com.japl.repzi.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.japl.repzi.feature.exercises.EXERCISES_ROUTE
import com.japl.repzi.feature.exercises.exercisesScreen
import com.japl.repzi.feature.history.historyScreen
import com.japl.repzi.feature.routines.routinesScreens
import com.japl.repzi.feature.session.navigateToSession
import com.japl.repzi.feature.session.sessionScreen

@Composable
fun RepziNavHost() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Secondary screens (like the routine editor) hide the bar.
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }

    Scaffold(
        // Each feature screen has its own Scaffold that handles the system bars; this one only adds the bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = EXERCISES_ROUTE,
            // consumeWindowInsets stops the inner Scaffolds from adding the bottom inset a second time.
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            exercisesScreen()
            routinesScreens(navController, onStartRoutine = navController::navigateToSession)
            sessionScreen(navController)
            historyScreen()
        }
    }
}
