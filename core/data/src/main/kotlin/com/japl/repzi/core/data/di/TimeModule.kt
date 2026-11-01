package com.japl.repzi.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.Clock

@Module
@InstallIn(SingletonComponent::class)
internal object TimeModule {

    // Injected instead of calling Clock.System directly so tests can control the current time.
    @Provides
    fun provideClock(): Clock = Clock.System
}
