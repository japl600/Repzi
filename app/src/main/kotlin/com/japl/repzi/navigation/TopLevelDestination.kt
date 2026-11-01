package com.japl.repzi.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.graphics.vector.ImageVector
import com.japl.repzi.R
import com.japl.repzi.feature.exercises.EXERCISES_ROUTE
import com.japl.repzi.feature.routines.ROUTINES_ROUTE

/** Destinations shown in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Exercises(EXERCISES_ROUTE, R.string.nav_exercises, Icons.AutoMirrored.Filled.List),
    Routines(ROUTINES_ROUTE, R.string.nav_routines, Icons.Default.DateRange),
}
