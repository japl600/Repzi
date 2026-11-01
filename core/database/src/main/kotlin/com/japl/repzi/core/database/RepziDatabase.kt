package com.japl.repzi.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.dao.RoutineDao
import com.japl.repzi.core.database.dao.WorkoutSessionDao
import com.japl.repzi.core.database.model.ExerciseEntity
import com.japl.repzi.core.database.model.LoggedSetEntity
import com.japl.repzi.core.database.model.RoutineEntity
import com.japl.repzi.core.database.model.RoutineExerciseEntity
import com.japl.repzi.core.database.model.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionEntity::class,
        LoggedSetEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class RepziDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
}
