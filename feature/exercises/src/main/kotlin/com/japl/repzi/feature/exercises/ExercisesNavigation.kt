package com.japl.repzi.feature.exercises

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

const val EXERCISES_ROUTE = "exercises"
internal const val EXERCISE_ID_ARG = "exerciseId"
private const val EXERCISE_DETAIL_ROUTE = "exercises/{$EXERCISE_ID_ARG}"

/** The exercise list and, behind it, the detail of one exercise. */
fun NavGraphBuilder.exercisesScreen(
    navController: NavController,
    onOpenProgress: (exerciseId: Long) -> Unit,
) {
    composable(EXERCISES_ROUTE) {
        ExercisesRoute(
            onOpenProgress = onOpenProgress,
            onOpenDetail = { exerciseId -> navController.navigate("exercises/$exerciseId") },
        )
    }
    composable(
        route = EXERCISE_DETAIL_ROUTE,
        arguments = listOf(navArgument(EXERCISE_ID_ARG) { type = NavType.LongType }),
    ) {
        ExerciseDetailRoute(onBack = { navController.popBackStack() })
    }
}
