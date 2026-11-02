package com.japl.repzi.feature.session

import com.japl.repzi.core.domain.model.Exercise
import java.io.Serializable

internal const val DRAFT_KEY = "draft"
internal const val STARTED_AT_KEY = "startedAt"

/**
 * What survives process death: only the text the user typed, keyed by exercise id. The routine itself is read again
 * from Room, so the saved state stays tiny (the Bundle has a ~1 MB transaction limit) and never goes stale.
 *
 * Plain [Serializable] classes are enough for a [androidx.lifecycle.SavedStateHandle] and avoid the Parcelize plugin.
 */
internal data class DraftExercise(val exerciseId: Long, val sets: ArrayList<DraftSet>) : Serializable

internal data class DraftSet(val weight: String, val reps: String) : Serializable

internal fun List<ExerciseLog>.toDraft() = ArrayList(
    map { log -> DraftExercise(log.exercise.id, ArrayList(log.sets.map { DraftSet(it.weight, it.reps) })) },
)

/**
 * Builds the exercise logs for [this] routine, refilling each one from the [draft] when it has an entry for the same
 * exercise. Exercises added to the routine since the draft was taken start empty; removed ones are dropped.
 */
internal fun List<Exercise>.toLogs(draft: List<DraftExercise>?): List<ExerciseLog> {
    val draftById = draft.orEmpty().associateBy { it.exerciseId }
    return map { exercise ->
        val sets = draftById[exercise.id]?.sets?.map { SetInput(it.weight, it.reps) }
        ExerciseLog(exercise, if (sets.isNullOrEmpty()) listOf(SetInput()) else sets)
    }
}
