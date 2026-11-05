package com.japl.repzi.feature.session

import com.japl.repzi.core.domain.model.DraftExercise
import com.japl.repzi.core.domain.model.DraftSet
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.WorkoutDraft
import kotlin.time.Instant

/** Only what the user typed is stored, keyed by exercise id: the routine itself is read again from Room. */
internal fun List<ExerciseLog>.toDraft(routineId: Long, startedAt: Instant) = WorkoutDraft(
    routineId = routineId,
    startedAt = startedAt,
    exercises = map { log -> DraftExercise(log.exercise.id, log.sets.map { DraftSet(it.weight, it.reps) }) },
)

/**
 * Builds the exercise logs for [this] routine, refilling each one from the [draft] when it has an entry for the same
 * exercise. Exercises added to the routine since the draft was taken start empty; removed ones are dropped.
 */
internal fun List<Exercise>.toLogs(draft: WorkoutDraft?): List<ExerciseLog> {
    val draftById = draft?.exercises.orEmpty().associateBy { it.exerciseId }
    return map { exercise ->
        val sets = draftById[exercise.id]?.sets?.map { SetInput(it.weight, it.reps) }
        ExerciseLog(exercise, if (sets.isNullOrEmpty()) listOf(SetInput()) else sets)
    }
}
