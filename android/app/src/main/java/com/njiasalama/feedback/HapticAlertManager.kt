package com.njiasalama.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.njiasalama.domain.proximity.ProximityAlertLevel

/**
 * Dispatches distinct tactile vibration patterns tailored to cut through cycling shorts,
 * trousers, and jerseys while the phone is stowed in the cyclist's pocket.
 */
class HapticAlertManager(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // Cooldown set: pin IDs that have already chimed during this ride session
    private val alertedPinIds = mutableSetOf<String>()

    /**
     * Triggers the appropriate tactile cadence against the cyclist's thigh.
     */
    fun triggerAlert(pinId: String, level: ProximityAlertLevel) {
        if (level == ProximityAlertLevel.NONE) return
        if (alertedPinIds.contains(pinId)) return

        alertedPinIds.add(pinId)

        when (level) {
            ProximityAlertLevel.NOTICE -> {
                // Single solid buzz (250ms) to alert the rider to look ahead
                vibratePattern(longArrayOf(0, 250))
            }
            ProximityAlertLevel.URGENT -> {
                // Aggressive double-pulse: [0ms delay, 150ms on, 80ms off, 300ms on]
                vibratePattern(longArrayOf(0, 150, 80, 300))
            }
            else -> {}
        }
    }

    /**
     * Resets alerted pins for a new ride session.
     */
    fun resetSession() {
        alertedPinIds.clear()
    }

    private fun vibratePattern(timings: LongArray) {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, -1)
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(timings, -1)
            }
        }
    }
}

