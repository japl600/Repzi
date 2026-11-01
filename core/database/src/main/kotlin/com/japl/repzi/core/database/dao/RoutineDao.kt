package com.japl.repzi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.japl.repzi.core.database.model.RoutineEntity
import com.japl.repzi.core.database.model.RoutineExerciseEntity
import com.japl.repzi.core.database.model.RoutineWithExercises
import kotlinx.coroutines.flow.Flow

// Abstract class instead of interface: @Transaction needs a method with a body.
@Dao
abstract class RoutineDao {

    @Transaction
    @Query("SELECT * FROM routines")
    abstract fun observeAll(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    abstract suspend fun getById(id: Long): RoutineWithExercises?

    /** Inserts or updates the routine and replaces its exercises, all in one transaction. */
    @Transaction
    open suspend fun save(routine: RoutineEntity, exerciseIds: List<Long>): Long {
        val id = if (routine.id == 0L) {
            insert(routine)
        } else {
            update(routine)
            routine.id
        }
        deleteItems(id)
        insertItems(exerciseIds.mapIndexed { position, exerciseId -> RoutineExerciseEntity(id, position, exerciseId) })
        return id
    }

    // The join rows go away with the routine thanks to ON DELETE CASCADE.
    @Query("DELETE FROM routines WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Insert
    protected abstract suspend fun insert(routine: RoutineEntity): Long

    @Update
    protected abstract suspend fun update(routine: RoutineEntity)

    @Insert
    protected abstract suspend fun insertItems(items: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercises WHERE routine_id = :routineId")
    protected abstract suspend fun deleteItems(routineId: Long)
}
