package com.japl.repzi.feature.routines

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.japl.repzi.core.domain.model.Routine

const val ROUTINES_ROUTE = "routines"

internal const val ROUTINE_ID_ARG = "routineId"
private const val ROUTINE_EDITOR_ROUTE = "routines/editor?$ROUTINE_ID_ARG={$ROUTINE_ID_ARG}"

fun NavController.navigateToRoutineEditor(routineId: Long = Routine.NEW_ID) {
    navigate("routines/editor?$ROUTINE_ID_ARG=$routineId")
}

fun NavGraphBuilder.routinesScreens(navController: NavController) {
    composable(ROUTINES_ROUTE) {
        RoutinesRoute(onEditRoutine = navController::navigateToRoutineEditor)
    }
    composable(
        route = ROUTINE_EDITOR_ROUTE,
        arguments = listOf(
            navArgument(ROUTINE_ID_ARG) {
                type = NavType.LongType
                defaultValue = Routine.NEW_ID
            },
        ),
    ) {
        RoutineEditorRoute(onFinished = navController::popBackStack)
    }
}
