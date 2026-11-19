package com.japl.repzi.feature.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mikepenz.aboutlibraries.Libs

const val SETTINGS_ROUTE = "settings"
const val SETTINGS_LICENSES_ROUTE = "settings/licenses"

/**
 * [libraries] loads the list of open-source libraries. It comes from the app module because the generated
 * resource (R.raw.aboutlibraries) lives there; it returns null while the list is loading.
 */
fun NavGraphBuilder.settingsScreens(
    navController: NavController,
    libraries: @Composable () -> Libs?,
) {
    composable(SETTINGS_ROUTE) {
        SettingsRoute(onOpenLicenses = { navController.navigate(SETTINGS_LICENSES_ROUTE) })
    }
    composable(SETTINGS_LICENSES_ROUTE) {
        LicensesScreen(libraries = libraries(), onBack = { navController.popBackStack() })
    }
}
