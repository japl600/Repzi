package com.japl.repzi.core.data.di

import com.japl.repzi.core.data.DefaultDispatcherProvider
import com.japl.repzi.core.data.repository.OfflineFirstExerciseRepository
import com.japl.repzi.core.domain.DispatcherProvider
import com.japl.repzi.core.domain.repository.ExerciseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindExerciseRepository(impl: OfflineFirstExerciseRepository): ExerciseRepository

    @Binds
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider
}
