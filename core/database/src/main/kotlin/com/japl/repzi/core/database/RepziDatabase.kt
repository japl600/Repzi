package com.japl.repzi.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.dao.RoutineDao
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.database.model.RoutineEntity
import com.japl.repzi.core.database.model.RoutineExerciseEntity

@Database(
    entities = [ExerciseEntity::class, RoutineEntity::class, RoutineExerciseEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class RepziDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
}
