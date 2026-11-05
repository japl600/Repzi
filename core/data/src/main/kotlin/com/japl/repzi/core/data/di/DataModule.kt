package com.japl.repzi.core.data.di

import com.japl.repzi.core.data.DefaultDispatcherProvider
import com.japl.repzi.core.data.repository.OfflineFirstDraftRepository
import com.japl.repzi.core.data.repository.OfflineFirstExerciseRepository
import com.japl.repzi.core.data.repository.OfflineFirstRoutineRepository
import com.japl.repzi.core.data.repository.OfflineFirstSessionRepository
import com.japl.repzi.core.domain.DispatcherProvider
import com.japl.repzi.core.domain.repository.DraftRepository
import com.japl.repzi.core.domain.repository.ExerciseRepository
import com.japl.repzi.core.domain.repository.RoutineRepository
import com.japl.repzi.core.domain.repository.SessionRepository
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
    abstract fun bindRoutineRepository(impl: OfflineFirstRoutineRepository): RoutineRepository

    @Binds
    abstract fun bindSessionRepository(impl: OfflineFirstSessionRepository): SessionRepository

    @Binds
    abstract fun bindDraftRepository(impl: OfflineFirstDraftRepository): DraftRepository

    @Binds
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider
}
