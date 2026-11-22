package com.japl.repzi.feature.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

private val Navy = Color(0xFF0E1B2C)
private val Teal = Color(0xFF2DD4BF)
private val Slate = Color(0xFF94A3B8)
private val Mist = Color(0xFFCBD5E1)

/**
 * The 2x2 widget: the last workout on top and the button below. Tapping anywhere but the button opens the app; the
 * button starts or resumes a routine, or opens the app when there is no routine yet.
 */
@Composable
fun HomeWidgetContent(
    texts: WidgetTexts,
    target: WidgetTarget,
    openApp: Action,
    startRoutine: (routineId: Long) -> Action,
) {
    val buttonAction = when (target) {
        is WidgetTarget.Start -> startRoutine(target.routineId)
        is WidgetTarget.Resume -> startRoutine(target.routineId)
        WidgetTarget.NoRoutines -> openApp
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Navy))
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(openApp),
    ) {
        Text(texts.title, style = TextStyle(color = ColorProvider(Slate), fontSize = 12.sp), maxLines = 1)
        texts.subtitle?.let {
            Text(it, style = TextStyle(color = ColorProvider(Mist), fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        }
        texts.summary?.let {
            Text(it, style = TextStyle(color = ColorProvider(Slate), fontSize = 12.sp), maxLines = 1)
        }
        Spacer(GlanceModifier.defaultWeight())
        Button(
            text = texts.button,
            onClick = buttonAction,
            modifier = GlanceModifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = ColorProvider(Teal), contentColor = ColorProvider(Navy)),
            maxLines = 1,
        )
    }
}
