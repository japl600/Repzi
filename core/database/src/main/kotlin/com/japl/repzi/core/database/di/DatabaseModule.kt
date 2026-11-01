package com.japl.repzi.core.database.di

import android.content.Context
import androidx.room.Room
import com.japl.repzi.core.database.ALL_MIGRATIONS
import com.japl.repzi.core.database.ExerciseSeedCallback
import com.japl.repzi.core.database.RepziDatabase
import com.japl.repzi.core.database.dao.ExerciseDao
import com.japl.repzi.core.database.dao.RoutineDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RepziDatabase =
        Room.databaseBuilder(context, RepziDatabase::class.java, "repzi.db")
            .addMigrations(*ALL_MIGRATIONS)
            .addCallback(ExerciseSeedCallback())
            .build()

    @Provides
    fun provideExerciseDao(database: RepziDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideRoutineDao(database: RepziDatabase): RoutineDao = database.routineDao()
}
