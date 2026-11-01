package com.japl.repzi.feature.session

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

internal const val ROUTINE_ID_ARG = "routineId"
private const val SESSION_ROUTE = "session/{$ROUTINE_ID_ARG}"

fun NavController.navigateToSession(routineId: Long) {
    navigate("session/$routineId")
}

fun NavGraphBuilder.sessionScreen(navController: NavController) {
    composable(
        route = SESSION_ROUTE,
        arguments = listOf(navArgument(ROUTINE_ID_ARG) { type = NavType.LongType }),
    ) {
        SessionRoute(onClose = navController::popBackStack)
    }
}
