package com.japl.repzi.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.japl.repzi.feature.exercises.EXERCISES_ROUTE
import com.japl.repzi.feature.exercises.exercisesScreen

@Composable
fun RepziNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = EXERCISES_ROUTE) {
        exercisesScreen()
        // routinesScreen(), sessionScreen(), historyScreen() se agregan aquí al implementar cada feature.
    }
}
