package com.japl.repzi.feature.widget

import com.japl.repzi.core.domain.usecase.ObserveRoutinesUseCase
import com.japl.repzi.core.domain.usecase.ObserveSessionsUseCase
import com.japl.repzi.core.domain.usecase.ObserveWeightUnitUseCase
import com.japl.repzi.core.domain.usecase.ObserveWorkoutDraftsUseCase
import javax.inject.Inject
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** The widget's [HomeWidgetState], recomputed whenever the sessions, routines, drafts or weight unit change. */
class ObserveHomeWidgetState @Inject constructor(
    private val sessions: ObserveSessionsUseCase,
    private val routines: ObserveRoutinesUseCase,
    private val drafts: ObserveWorkoutDraftsUseCase,
    private val weightUnit: ObserveWeightUnitUseCase,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<HomeWidgetState> =
        combine(sessions(), routines(), drafts(), weightUnit()) { sessions, routines, drafts, unit ->
            homeWidgetState(sessions, routines, drafts, unit, clock.now())
        }
}
