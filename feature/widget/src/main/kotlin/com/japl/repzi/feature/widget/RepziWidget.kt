package com.japl.repzi.feature.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.ui.navigation.sessionDeepLink
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

/** The 2x2 home-screen widget: the last workout and a button that starts (or resumes) the routine up next. */
class RepziWidget : GlanceAppWidget() {

    // One layout for every size the user stretches it to.
    override val sizeMode = SizeMode.Single

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface Dependencies {
        fun observeHomeWidgetState(): ObserveHomeWidgetState
        fun clock(): Clock
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dependencies = EntryPointAccessors.fromApplication(context.applicationContext, Dependencies::class.java)
        val observeState = dependencies.observeHomeWidgetState()
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)

        // While Glance keeps this session open (it does for a while after each update), an update only recomposes
        // the content: this function does not run again. So the state is collected inside the content, where it keeps
        // changing, and read once here to have something to draw from the first frame.
        val initialState = try {
            observeState().first()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not read the widget's state", e)
            EMPTY_STATE
        }

        provideContent {
            val state by remember { observeState().catch { Log.w(TAG, "The widget's state failed", it); emit(EMPTY_STATE) } }
                .collectAsState(initial = initialState)
            HomeWidgetContent(
                texts = widgetTexts(context, state, dependencies.clock().now(), ZoneId.systemDefault()),
                target = state.target,
                openApp = openAppAction(launchIntent),
                startRoutine = { routineId -> startRoutineAction(launchIntent, routineId) },
            )
        }
    }

    private fun openAppAction(launchIntent: Intent?): Action = actionStartActivity(launchIntent ?: Intent())

    /** An explicit intent for the launcher activity carrying the deep link, so no manifest intent filter is needed. */
    private fun startRoutineAction(launchIntent: Intent?, routineId: Long): Action {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sessionDeepLink(routineId)))
        launchIntent?.component?.let(intent::setComponent)
        return actionStartActivity(intent)
    }

    private companion object {
        const val TAG = "RepziWidget"

        // What the home screen draws when the real state cannot be read: the empty widget, which opens the app.
        val EMPTY_STATE = HomeWidgetState(lastWorkout = null, target = WidgetTarget.NoRoutines, weightUnit = WeightUnit.KG)
    }
}
