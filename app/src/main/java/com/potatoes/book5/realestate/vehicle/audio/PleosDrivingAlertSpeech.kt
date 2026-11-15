package com.potatoes.book5.realestate.vehicle.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.domain.port.DrivingAlertSpeechKind
import com.potatoes.book5.realestate.domain.port.DrivingAlertSpeech
import java.util.Locale

/** One bounded on-device utterance; callbacks from a replaced request cannot stop a newer one. */
class PleosDrivingAlertSpeech(context: Context) : DrivingAlertSpeech {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private var foreground = false
    private var allowedKinds = emptySet<DrivingAlertSpeechKind>()
    private var active: Session? = null
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    fun setForeground(value: Boolean) {
        handler.post {
            foreground = value
            if (!value) closeActive()
        }
    }

    fun setAllowed(recommendation: Boolean, leader: Boolean) {
        handler.post {
            allowedKinds = buildSet {
                if (recommendation) add(DrivingAlertSpeechKind.RECOMMENDATION)
                if (leader) add(DrivingAlertSpeechKind.LEADER)
            }
            if (active?.kind !in allowedKinds) closeActive()
        }
    }

    override fun announce(key: Long, apartmentName: String, kind: DrivingAlertSpeechKind) {
        handler.post {
            closeActive()
            if (!foreground || kind !in allowedKinds || apartmentName.isBlank()) return@post
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) ||
                context.checkSelfPermission(TTS_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                event("unavailable")
                return@post
            }
            try {
                val session = Session(key, kind)
                active = session
                // No delayed playback after a banner disappears, even if initialization hangs.
                handler.postDelayed(session.expire, MAX_REQUEST_MILLIS)
                session.engine.initialize(object : OnInitializeListener {
                    override fun onSuccess() {
                        handler.post {
                            if (active !== session) return@post
                            try {
                                session.engine.addEventListener(session.listener)
                                session.engine.setLocale(Locale.KOREA)
                                session.engine.setAudioAttributes(attributes)
                                session.hasFocus = audio?.requestAudioFocus(session.focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                                if (!session.hasFocus) {
                                    event("focus_denied")
                                    finish(session)
                                    return@post
                                }
                                val resource = when (kind) {
                                    DrivingAlertSpeechKind.RECOMMENDATION -> R.string.recommendation_found_speech
                                    DrivingAlertSpeechKind.LEADER -> R.string.leader_found_speech
                                }
                                session.engine.speak(context.getString(resource, apartmentName), true)
                                event("requested kind=${kind.name}")
                            } catch (_: Exception) {
                                event("failed")
                                finish(session)
                            }
                        }
                    }

                    override fun onFailure(errorType: ErrorType) {
                        handler.post { event("unavailable"); finish(session) }
                    }
                })
            } catch (_: Exception) {
                event("unavailable")
                closeActive()
            }
        }
    }

    override fun stop(key: Long?) {
        handler.post { if (key == null || active?.key == key) closeActive() }
    }

    private inner class Session(val key: Long, val kind: DrivingAlertSpeechKind) {
        val engine = TextToSpeech(context, Mode.ON_DEVICE)
        var hasFocus = false
        val expire = Runnable { finish(this) }
        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener({ change ->
                if (change != AudioManager.AUDIOFOCUS_GAIN) {
                    // Includes Gleo's LOSS_TRANSIENT; never resume an obsolete notification.
                    event("focus_lost")
                    finish(this)
                }
            }, handler)
            .build()
        val listener = object : EventListener {
            override fun onReady() = Unit
            override fun onUpdatedRms(rms: Double) = Unit
            override fun onStart() { handler.post { if (active === this@Session) event("started") } }
            override fun onDone() { handler.post { if (active === this@Session) event("done"); finish(this@Session) } }
            override fun onStop() { handler.post { finish(this@Session) } }
            override fun onError(errorType: ErrorType) { handler.post { if (active === this@Session) event("failed"); finish(this@Session) } }
        }
    }

    private fun finish(session: Session) {
        if (active === session) closeActive()
    }

    private fun closeActive() {
        val session = active ?: return
        active = null
        handler.removeCallbacks(session.expire)
        runCatching { session.engine.removeEventListener(session.listener) }
        runCatching { session.engine.stop() }
        runCatching { session.engine.release() }
        if (session.hasFocus) runCatching { audio?.abandonAudioFocusRequest(session.focus) }
    }

    private fun event(value: String) { Log.i("RecommendationTts", "event=$value") }

    private companion object {
        const val TTS_PERMISSION = "pleos.car.permission.TTS_SERVICE"
        const val MAX_REQUEST_MILLIS = 10_000L
    }
}
