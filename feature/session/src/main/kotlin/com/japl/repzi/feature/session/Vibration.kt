package com.japl.repzi.feature.session

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

private val REST_OVER_PATTERN = longArrayOf(0, 250, 150, 250)

/** Two short buzzes, the signal that the rest is over. Does nothing on a device without a vibrator. */
internal fun Context.buzzRestOver() {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    if (vibrator?.hasVibrator() == true) {
        // A vibration with no declared usage is "unknown", and Android drops it when the user has turned off touch and
        // media vibration (as many phones have). The end of a rest is a timer alert, so it is declared as an alarm.
        val alarm = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        @Suppress("DEPRECATION")
        vibrator.vibrate(VibrationEffect.createWaveform(REST_OVER_PATTERN, NO_REPEAT), alarm)
    }
}

private const val NO_REPEAT = -1
