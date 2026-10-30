package com.japl.repzi.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.model.ExerciseEntity

@Database(
    entities = [ExerciseEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RepziDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
}
