package com.japl.repzi.core.data.repository

import com.japl.repzi.core.data.ExerciseNames
import com.japl.repzi.core.data.mapper.toDomain
import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.domain.DispatcherProvider
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.repository.ExerciseRepository
import java.text.Collator
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Room is the single source of truth; once there is a network, it is synced into the local database. */
internal class OfflineFirstExerciseRepository @Inject constructor(
    private val dao: ExerciseDao,
    private val dispatchers: DispatcherProvider,
    private val names: ExerciseNames,
) : ExerciseRepository {

    override fun observeExercises(muscleGroup: MuscleGroup?): Flow<List<Exercise>> {
        val entities = if (muscleGroup == null) {
            dao.observeAll()
        } else {
            dao.observeByMuscleGroup(muscleGroup.name)
        }
        // The shown name depends on the language, so the order is decided here and not by the query.
        return entities.map { list ->
            val collator = Collator.getInstance()
            list.map { it.toDomain(names) }.sortedWith(compareBy(collator) { it.name })
        }
    }

    override suspend fun addCustomExercise(name: String, muscleGroup: MuscleGroup) {
        withContext(dispatchers.io) {
            dao.insert(ExerciseEntity(name = name.trim(), muscleGroup = muscleGroup.name, isCustom = true))
        }
    }
}
