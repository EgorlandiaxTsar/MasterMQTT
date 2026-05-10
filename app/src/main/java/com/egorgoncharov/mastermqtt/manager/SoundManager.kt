package com.egorgoncharov.mastermqtt.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.net.toUri
import com.egorgoncharov.mastermqtt.Utils.Companion.resolveJsonTemplates
import com.egorgoncharov.mastermqtt.model.dao.SettingsProfileDao
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.model.types.TTSLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

open class SoundManager(protected val context: Context, settingsProfileDao: SettingsProfileDao) {
    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    protected val settingsFlow = settingsProfileDao.streamMainSettingsProfile()
    protected var preferredLanguage = TTSLanguage.EN
    protected var tts: TextToSpeech? = null
    protected var isTtsReady = false
    protected val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        initTts()
        scope.launch {
            settingsFlow.collect { mainSettingsProfile ->
                if (mainSettingsProfile == null) return@collect
                if (preferredLanguage.locale.language != mainSettingsProfile.ttsLanguage.locale.language) {
                    preferredLanguage = mainSettingsProfile.ttsLanguage
                    updateTtsLanguage()
                }
            }
        }
    }

    private fun initTts() {
        tts?.shutdown()
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) updateTtsLanguage() else isTtsReady = false
        }
    }

    private fun updateTtsLanguage() {
        val result = tts?.setLanguage(preferredLanguage.locale)
        isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
    }

    suspend fun alert(topic: TopicEntity, payload: String? = null) {
        if (!topic.notificationSoundPath.isNullOrBlank() && topic.notificationSoundLevel != null) {
            playSound(topic.notificationSoundPath, topic.notificationSoundLevel, topic.highPriority, topic.ignoreBedTime)
        }
        var textToSpeak = topic.notificationSoundText
        if (!textToSpeak.isNullOrBlank() && !payload.isNullOrBlank() && textToSpeak.contains("={")) {
            textToSpeak = resolveJsonTemplates(textToSpeak, payload)
        }
        if (!textToSpeak.isNullOrBlank() && topic.notificationSoundLevel != null) {
            speak(textToSpeak, topic.notificationSoundLevel, topic.highPriority, topic.ignoreBedTime)
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isTtsReady = false
    }

    open suspend fun playSound(path: String, volume: Double = 1.0, highPriority: Boolean = false, bypassDnd: Boolean = false) {
        playSoundInternal(mediaPlayerFrom(path), volume, highPriority, bypassDnd)
    }

    open suspend fun playSound(resourceId: Int, volume: Double = 1.0, highPriority: Boolean = false, bypassDnd: Boolean = false) {
        playSoundInternal(mediaPlayerFrom(resourceId), volume, highPriority, bypassDnd)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    protected open suspend fun speak(text: String, volume: Double = 1.0, highPriority: Boolean = false, bypassDnd: Boolean = false) {
        if (!isTtsReady) {
            suspendCancellableCoroutine { cont ->
                tts?.shutdown()
                tts = TextToSpeech(context) { status ->
                    isTtsReady = (status == TextToSpeech.SUCCESS)
                    if (isTtsReady) updateTtsLanguage()
                    cont.resume(Unit)
                }
            }
        }
        if (!isTtsReady) return
        val requireAudioFocus = bypassDnd || highPriority
        val streamType = AudioManager.STREAM_NOTIFICATION
        val originalVolume = audioManager.getStreamVolume(streamType)
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val targetVolume = (maxVolume * volume).toInt()
        suspendCancellableCoroutine { cont ->
            val utteranceId = "mqttAlert_${System.currentTimeMillis()}"
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                private fun cleanup() {
                    if (requireAudioFocus) audioManager.abandonAudioFocus(null)
                    audioManager.setStreamVolume(streamType, originalVolume, 0)
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) {
                    if (id == utteranceId) cleanup()
                }

                @Deprecated("Still required by the abstract class on older APIs")
                override fun onError(id: String?) {
                    if (id == utteranceId) cleanup()
                }

                override fun onError(id: String?, errorCode: Int) {
                    if (id == utteranceId) cleanup()
                }
            })
            if (requireAudioFocus) {
                audioManager.requestAudioFocus(
                    null,
                    streamType,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
            audioManager.setStreamVolume(streamType, targetVolume, 0)
            val params = Bundle()
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, streamType)
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (result == TextToSpeech.ERROR) {
                if (requireAudioFocus) audioManager.abandonAudioFocus(null)
                audioManager.setStreamVolume(streamType, originalVolume, 0)
                initTts()
                cont.resume(Unit)
            }
            cont.invokeOnCancellation {
                if (requireAudioFocus) audioManager.abandonAudioFocus(null)
                audioManager.setStreamVolume(streamType, originalVolume, 0)
                tts?.stop()
            }
        }
    }

    private suspend fun playSoundInternal(player: MediaPlayer, volume: Double = 1.0, highPriority: Boolean = false, bypassDnd: Boolean = false) {
        val streamType = if (bypassDnd) AudioManager.STREAM_ALARM else AudioManager.STREAM_NOTIFICATION
        val requireAudioFocus = bypassDnd || highPriority
        val originalVolume = audioManager.getStreamVolume(streamType)
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val targetVolume = (maxVolume * volume).toInt()
        return suspendCancellableCoroutine { cont ->
            try {
                player.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(if (bypassDnd) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setVolume(1.0f, 1.0f)
                    setOnCompletionListener {
                        it.release()
                        audioManager.setStreamVolume(streamType, originalVolume, 0)
                        if (requireAudioFocus) audioManager.abandonAudioFocus(null)
                        if (cont.isActive) cont.resume(Unit)
                    }
                    setOnErrorListener { mp, _, _ ->
                        mp.release()
                        audioManager.setStreamVolume(streamType, originalVolume, 0)
                        if (requireAudioFocus) audioManager.abandonAudioFocus(null)
                        if (cont.isActive) cont.resume(Unit)
                        true
                    }
                    prepare()
                }
                if (requireAudioFocus) {
                    audioManager.requestAudioFocus(
                        null,
                        AudioManager.STREAM_NOTIFICATION,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                    )
                }
                audioManager.setStreamVolume(streamType, targetVolume, 0)
                player.start()
                cont.invokeOnCancellation {
                    audioManager.setStreamVolume(streamType, originalVolume, 0)
                    player.stop()
                    player.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                audioManager.setStreamVolume(streamType, originalVolume, 0)
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    private fun mediaPlayerFrom(path: String): MediaPlayer {
        return MediaPlayer().apply { setDataSource(context, path.toUri()) }
    }

    private fun mediaPlayerFrom(resourceId: Int): MediaPlayer {
        return MediaPlayer().apply { setDataSource(context, ("android.resource://" + context.packageName + "/" + resourceId).toUri()) }
    }
}