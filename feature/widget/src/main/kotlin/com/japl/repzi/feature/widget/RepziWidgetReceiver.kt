package com.japl.repzi.feature.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** What the system calls to add, update and remove the widget; it hands the drawing to [RepziWidget]. */
class RepziWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RepziWidget()
}
