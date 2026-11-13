package com.japl.repzi.core.data.repository

import com.japl.repzi.core.data.ExerciseNames
import com.japl.repzi.core.data.mapper.toDomain
import com.japl.repzi.core.data.mapper.toEntity
import com.japl.repzi.core.database.dao.RoutineDao
import com.japl.repzi.core.domain.DispatcherProvider
import com.japl.repzi.core.domain.model.Routine
import com.japl.repzi.core.domain.repository.RoutineRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class OfflineFirstRoutineRepository @Inject constructor(
    private val dao: RoutineDao,
    private val dispatchers: DispatcherProvider,
    private val names: ExerciseNames,
) : RoutineRepository {

    override fun observeRoutines(): Flow<List<Routine>> =
        dao.observeAll().map { list -> list.map { it.toDomain(names) } }

    override suspend fun getRoutine(id: Long): Routine? = dao.getById(id)?.toDomain(names)

    override suspend fun saveRoutine(routine: Routine): Long = withContext(dispatchers.io) {
        dao.save(routine.toEntity(), routine.exercises.map { it.id })
    }

    override suspend fun deleteRoutine(id: Long) = withContext(dispatchers.io) {
        dao.delete(id)
    }
}
