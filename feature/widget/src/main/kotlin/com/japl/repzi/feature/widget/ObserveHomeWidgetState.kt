package com.japl.repzi.feature.widget

import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.model.WeightUnit
import com.japl.repzi.core.domain.model.WorkoutDraftInfo
import com.japl.repzi.core.domain.model.WorkoutSession
import com.japl.repzi.core.domain.usecase.GetWorkoutDraftUseCase
import com.japl.repzi.core.domain.usecase.ObserveRoutinesUseCase
import com.japl.repzi.core.domain.usecase.ObserveSessionsUseCase
import com.japl.repzi.core.domain.usecase.ObserveWeightUnitUseCase
import com.japl.repzi.core.domain.usecase.ObserveWorkoutDraftsUseCase
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformLatest

/** The language code (`"en"`, `"es"`) the app is showing at this moment. */
fun interface WidgetLanguage {
    fun current(): String
}

/**
 * The widget's [HomeWidgetState], recomputed whenever the sessions, routines, drafts or weight unit change, and again
 * when a draft being offered for resuming gets too old, which changes nothing in the database.
 *
 * The sessions and routines also emit again when the app language changes (their exercise names are mapped per
 * language), so the state, which carries the language, comes out different and the widget gets redrawn.
 */
class ObserveHomeWidgetState @Inject constructor(
    private val sessions: ObserveSessionsUseCase,
    private val routines: ObserveRoutinesUseCase,
    private val drafts: ObserveWorkoutDraftsUseCase,
    private val weightUnit: ObserveWeightUnitUseCase,
    private val clock: Clock,
    private val language: WidgetLanguage,
) {
    private class Inputs(
        val sessions: List<WorkoutSession>,
        val routines: List<Routine>,
        val drafts: List<WorkoutDraftInfo>,
        val weightUnit: WeightUnit,
    )

    operator fun invoke(): Flow<HomeWidgetState> =
        combine(sessions(), routines(), drafts(), weightUnit()) { sessions, routines, drafts, unit ->
            Inputs(sessions, routines, drafts, unit)
        }.transformLatest { inputs ->
            while (true) {
                val now = clock.now()
                emit(
                    homeWidgetState(inputs.sessions, inputs.routines, inputs.drafts, inputs.weightUnit, now)
                        .copy(language = language.current()),
                )
                // A draft stops being resumable once it is older than MAX_AGE; wake up right after the next one does.
                val nextExpiry = inputs.drafts.map { it.startedAt + GetWorkoutDraftUseCase.MAX_AGE }.filter { it > now }.minOrNull()
                    ?: break
                delay(nextExpiry - now + 1.milliseconds)
            }
        }
}
