package com.japl.repzi.feature.history

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val HISTORY_ROUTE = "history"

fun NavGraphBuilder.historyScreen(onOpenProgress: () -> Unit) {
    composable(HISTORY_ROUTE) { HistoryRoute(onOpenProgress = onOpenProgress) }
}
