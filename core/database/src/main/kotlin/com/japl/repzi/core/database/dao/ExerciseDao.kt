package com.japl.repzi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.japl.repzi.core.database.model.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE muscle_group = :muscleGroup ORDER BY name")
    fun observeByMuscleGroup(muscleGroup: String): Flow<List<ExerciseEntity>>

    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long
}
