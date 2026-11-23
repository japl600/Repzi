package com.japl.repzi.feature.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
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
            Text(it, style = TextStyle(color = ColorProvider(Mist), fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 2)
        }
        texts.summary?.let {
            Text(it, style = TextStyle(color = ColorProvider(Slate), fontSize = 12.sp), maxLines = 1)
        }
        Spacer(GlanceModifier.defaultWeight())
        ActionButton(texts.button, showPlay = target !is WidgetTarget.NoRoutines, onClick = buttonAction)
    }
}

/**
 * Glance 1.2's Button has no icon, so this is a row with the play triangle and the text. The triangle says "go" at a
 * glance; with no routine the button only opens the app, so it has none.
 */
@Composable
private fun ActionButton(text: String, showPlay: Boolean, onClick: Action) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(Teal))
            .cornerRadius(24.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showPlay) {
            Image(ImageProvider(R.drawable.ic_widget_play), contentDescription = null, modifier = GlanceModifier.size(18.dp))
            Spacer(GlanceModifier.width(6.dp))
        }
        // Two lines: the routine's name is the point of the button, and a one-line label cuts it ("Empezar E…").
        Text(text, style = TextStyle(color = ColorProvider(Navy), fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 2)
    }
}
