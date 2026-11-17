package com.japl.repzi.core.domain.usecase

import com.japl.repzi.core.domain.model.PredefinedRoutine
import com.japl.repzi.core.domain.model.RestTimer
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.model.RoutineExercise
import com.japl.repzi.core.domain.repository.ExerciseRepository
import com.japl.repzi.core.domain.repository.RoutineRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed interface AddPredefinedRoutineResult {
    /** [name] is the name the routine was saved with, which may carry a number when the user already had that name. */
    data class Added(val id: Long, val name: String) : AddPredefinedRoutineResult

    /** None of the template's exercises exist in this catalog, so there was nothing to save. */
    data object Unavailable : AddPredefinedRoutineResult
}

/**
 * Copies a [PredefinedRoutine] into the user's routines. The caller passes the [name] in the language the app uses, so
 * this stays free of Android resources. Adding the same template twice creates a second routine, numbered
 * "Push (2)", so it never blocks the user.
 */
class AddPredefinedRoutineUseCase @Inject constructor(
    private val exercises: ExerciseRepository,
    private val routines: RoutineRepository,
    private val saveRoutine: SaveRoutineUseCase,
) {
    suspend operator fun invoke(template: PredefinedRoutine, name: String): AddPredefinedRoutineResult {
        val bySeedKey = exercises.observeExercises().first()
            .mapNotNull { exercise -> exercise.seedKey?.let { it to exercise } }
            .toMap()
        // A key this build does not have is skipped and the rest of the routine is still useful.
        val items = template.exerciseKeys.mapNotNull(bySeedKey::get).map { exercise ->
            RoutineExercise(exercise, RestTimer.defaultFor(exercise.muscleGroup))
        }
        if (items.isEmpty()) return AddPredefinedRoutineResult.Unavailable

        val finalName = availableName(name.trim(), taken = routines.observeRoutines().first().map { it.name })
        return when (val result = saveRoutine(Routine(name = finalName, exercises = items))) {
            is SaveRoutineResult.Saved -> AddPredefinedRoutineResult.Added(result.id, finalName)
            // Not reachable with a non-blank name and a non-empty list, but the type forces an answer.
            SaveRoutineResult.BlankName, SaveRoutineResult.NoExercises -> AddPredefinedRoutineResult.Unavailable
        }
    }

    private fun availableName(name: String, taken: List<String>): String {
        val used = taken.map { it.trim().lowercase() }.toSet()
        if (name.lowercase() !in used) return name
        return generateSequence(2) { it + 1 }
            .map { "$name ($it)" }
            .first { it.lowercase() !in used }
    }
}
