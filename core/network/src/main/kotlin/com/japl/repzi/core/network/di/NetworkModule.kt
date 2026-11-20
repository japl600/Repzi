package com.japl.repzi.core.network.di

import com.japl.repzi.core.network.RetrofitWgerClient
import com.japl.repzi.core.network.WGER_BASE_URL
import com.japl.repzi.core.network.WgerApi
import com.japl.repzi.core.network.WgerClient
import com.japl.repzi.core.network.createHttpClient
import com.japl.repzi.core.network.createWgerApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkBindingsModule {

    @Binds
    abstract fun bindWgerClient(impl: RetrofitWgerClient): WgerClient
}

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Provides
    @Singleton
    fun provideWgerApi(): WgerApi = createWgerApi(WGER_BASE_URL, createHttpClient())
}
