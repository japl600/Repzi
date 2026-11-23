package com.japl.repzi.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object WidgetModule {

    @Provides
    fun provideWidgetRefresher(@ApplicationContext context: Context): WidgetRefresher =
        WidgetRefresher { RepziWidget().updateAll(context) }

    @Provides
    fun provideWidgetLanguage(@ApplicationContext context: Context): WidgetLanguage =
        WidgetLanguage { context.resources.configuration.locales[0].language }
}
