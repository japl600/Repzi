package com.japl.repzi.feature.exercises

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val EXERCISES_ROUTE = "exercises"

fun NavGraphBuilder.exercisesScreen() {
    composable(EXERCISES_ROUTE) { ExercisesRoute() }
}
