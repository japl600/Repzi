package com.japl.repzi.navigation

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.test.platform.app.InstrumentationRegistry
import com.japl.repzi.core.ui.navigation.SESSION_DEEP_LINK_PATTERN
import com.japl.repzi.core.ui.navigation.sessionDeepLink
import kotlin.test.Test
import org.junit.Rule

/**
 * The home-screen widget starts a workout by sending a link to the activity. When the app is already open the activity
 * is reused (`singleTop`) and gets the link as a new intent, which Navigation Compose does not read by itself.
 *
 * The session destination here is a stand-in with the real link pattern: the real one needs Hilt view models, and what
 * is under test is the wiring between the new intent and the NavController.
 */
class DeepLinksFromNewIntentsTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun showNavHost(): NavHostController {
        lateinit var navController: NavHostController
        rule.setContent {
            navController = rememberNavController()
            DeepLinksFromNewIntents(navController)
            NavHost(navController, startDestination = "home") {
                composable("home") { Text("Home") }
                composable(
                    route = "session/{routineId}",
                    arguments = listOf(navArgument("routineId") { type = NavType.LongType }),
                    deepLinks = listOf(navDeepLink { uriPattern = SESSION_DEEP_LINK_PATTERN }),
                ) { entry -> Text("Session ${entry.arguments?.getLong("routineId")}") }
            }
        }
        return navController
    }

    private fun deliver(intent: Intent) {
        rule.runOnUiThread {
            InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(rule.activity, intent)
        }
    }

    @Test
    fun aLinkSentToTheRunningActivityOpensItsWorkout() {
        showNavHost()
        rule.onNodeWithText("Home").assertIsDisplayed()

        deliver(Intent(Intent.ACTION_VIEW, Uri.parse(sessionDeepLink(7))))

        rule.onNodeWithText("Session 7").assertIsDisplayed()
    }

    @Test
    fun anIntentThatIsNotALinkLeavesTheScreenAlone() {
        showNavHost()

        deliver(Intent(Intent.ACTION_MAIN))

        rule.onNodeWithText("Home").assertIsDisplayed()
    }
}
