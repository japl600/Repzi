package com.japl.repzi.core.data.repository

import com.japl.repzi.core.data.mapper.toDomain
import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.domain.DispatcherProvider
import com.japl.repzi.core.domain.model.Exercise
import com.japl.repzi.core.domain.model.MuscleGroup
import com.japl.repzi.core.domain.repository.ExerciseRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Room es la única fuente de verdad; cuando exista red, se sincroniza hacia la base local. */
internal class OfflineFirstExerciseRepository @Inject constructor(
    private val dao: ExerciseDao,
    private val dispatchers: DispatcherProvider,
) : ExerciseRepository {

    override fun observeExercises(muscleGroup: MuscleGroup?): Flow<List<Exercise>> {
        val entities = if (muscleGroup == null) {
            dao.observeAll()
        } else {
            dao.observeByMuscleGroup(muscleGroup.name)
        }
        return entities.map { list -> list.map(ExerciseEntity::toDomain) }
    }

    override suspend fun addCustomExercise(name: String, muscleGroup: MuscleGroup) {
        withContext(dispatchers.io) {
            dao.insert(ExerciseEntity(name = name.trim(), muscleGroup = muscleGroup.name, isCustom = true))
        }
    }
}
