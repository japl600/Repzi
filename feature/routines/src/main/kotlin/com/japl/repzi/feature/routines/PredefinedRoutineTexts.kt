package com.japl.repzi.feature.routines

import androidx.annotation.StringRes

internal data class PredefinedRoutineTexts(
    @param:StringRes val name: Int,
    @param:StringRes val description: Int,
)

/** One entry per key in [com.japl.repzi.core.domain.model.PredefinedRoutines]; a test checks they match. */
internal val PREDEFINED_ROUTINE_TEXTS: Map<String, PredefinedRoutineTexts> = mapOf(
    "full_body" to PredefinedRoutineTexts(R.string.predefined_full_body_name, R.string.predefined_full_body_description),
    "push" to PredefinedRoutineTexts(R.string.predefined_push_name, R.string.predefined_push_description),
    "pull" to PredefinedRoutineTexts(R.string.predefined_pull_name, R.string.predefined_pull_description),
    "legs" to PredefinedRoutineTexts(R.string.predefined_legs_name, R.string.predefined_legs_description),
    "core" to PredefinedRoutineTexts(R.string.predefined_core_name, R.string.predefined_core_description),
)
