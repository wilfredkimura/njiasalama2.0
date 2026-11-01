package com.njiasalama.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import com.njiasalama.domain.model.HazardType
import com.njiasalama.domain.proximity.ProximityAlert
import com.njiasalama.domain.proximity.ProximityAlertLevel
import java.util.Locale

/**
 * Manages audible voice alerts for cyclists using earbuds or the phone speaker.
 * Requests transient audio focus so that background music or podcasts duck cleanly
 * while the hazard is spoken into the cyclist's ear.
 */
class AudioAlertManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private var focusRequest: AudioFocusRequest? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setAudioAttributes(audioAttributes)
            isTtsReady = true
        }
    }

    /**
     * Speaks an audio warning into the cyclist's earbud or speaker, ducking media if playing.
     */
    fun speakHazardAlert(alert: ProximityAlert) {
        if (!isTtsReady || tts == null) return

        val hazardName = when (alert.pin.type) {
            HazardType.POTHOLE -> "Pothole"
            HazardType.UNLIT_STREET -> "Unlit street"
            HazardType.DANGEROUS_TRAFFIC -> "Heavy traffic hazard"
            HazardType.OTHER -> "Road obstacle"
        }

        val distanceRounded = (alert.distanceMeters / 10).toInt() * 10
        val speechText = when (alert.level) {
            ProximityAlertLevel.URGENT -> "Caution: $hazardName right ahead!"
            ProximityAlertLevel.NOTICE -> "Caution: $hazardName in $distanceRounded meters."
            else -> return
        }

        requestAudioFocusAndSpeak(speechText)
    }

    private fun requestAudioFocusAndSpeak(text: String) {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener { /* No-op */ }
                    .build()
                am.requestAudioFocus(focusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hazard_alert_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
