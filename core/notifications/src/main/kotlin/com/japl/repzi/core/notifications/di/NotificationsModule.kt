package com.japl.repzi.core.notifications.di

import com.japl.repzi.core.domain.AppVisibility
import com.japl.repzi.core.domain.RestReminderScheduler
import com.japl.repzi.core.notifications.AlarmManagerRestReminderScheduler
import com.japl.repzi.core.notifications.InactivityNotifier
import com.japl.repzi.core.notifications.NotificationGate
import com.japl.repzi.core.notifications.NotificationManagerInactivityNotifier
import com.japl.repzi.core.notifications.ProcessAppVisibility
import com.japl.repzi.core.notifications.SystemNotificationGate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationsModule {

    @Binds
    abstract fun bindRestReminderScheduler(impl: AlarmManagerRestReminderScheduler): RestReminderScheduler

    @Binds
    abstract fun bindAppVisibility(impl: ProcessAppVisibility): AppVisibility

    @Binds
    abstract fun bindNotificationGate(impl: SystemNotificationGate): NotificationGate

    @Binds
    abstract fun bindInactivityNotifier(impl: NotificationManagerInactivityNotifier): InactivityNotifier
}
