package com.japl.repzi.feature.progress

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

internal const val EXERCISE_ID_ARG = "exerciseId"
internal const val NO_EXERCISE = -1L
private const val PROGRESS_ROUTE = "progress?$EXERCISE_ID_ARG={$EXERCISE_ID_ARG}"

/** Opens the progress screen, on [exerciseId] when given or else on the first exercise that has progress. */
fun NavController.navigateToProgress(exerciseId: Long? = null) {
    navigate("progress?$EXERCISE_ID_ARG=${exerciseId ?: NO_EXERCISE}")
}

fun NavGraphBuilder.progressScreen(navController: NavController) {
    composable(
        route = PROGRESS_ROUTE,
        arguments = listOf(
            navArgument(EXERCISE_ID_ARG) {
                type = NavType.LongType
                defaultValue = NO_EXERCISE
            },
        ),
    ) {
        ProgressRoute(onBack = navController::popBackStack)
    }
}
