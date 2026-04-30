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

open class SoundManager(protected val context: Context, protected val settingsProfileDao: SettingsProfileDao) {
    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    protected val settingsFlow = settingsProfileDao.streamMainSettingsProfile()
    protected var preferredLanguage = TTSLanguage.EN
    protected var tts: TextToSpeech? = null
    protected var isTtsReady = false

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
            playSound(topic.notificationSoundPath, topic.notificationSoundLevel, topic.ignoreBedTime)
        }
        var textToSpeak = topic.notificationSoundText
        if (!textToSpeak.isNullOrBlank() && !payload.isNullOrBlank() && textToSpeak.contains("={")) {
            textToSpeak = resolveJsonTemplates(textToSpeak, payload)
        }
        if (!textToSpeak.isNullOrBlank()) {
            speak(textToSpeak)
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isTtsReady = false
    }

    open suspend fun playSound(path: String, volume: Double = 1.0, bypassDnd: Boolean = false) {
        return suspendCancellableCoroutine { cont ->
            try {
                val mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, path.toUri())
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(if (bypassDnd) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setVolume(volume.toFloat(), volume.toFloat())
                    setOnCompletionListener {
                        it.release()
                        if (cont.isActive) cont.resume(Unit)
                    }
                    setOnErrorListener { mp, _, _ ->
                        mp.release()
                        if (cont.isActive) cont.resume(Unit)
                        true
                    }
                    prepare()
                }
                mediaPlayer.start()
                cont.invokeOnCancellation {
                    mediaPlayer.stop()
                    mediaPlayer.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    protected open suspend fun speak(text: String) {
//        if (!isTtsReady) {
//            suspendCancellableCoroutine { cont ->
//                tts?.shutdown()
//                tts = TextToSpeech(context) { status ->
//                    isTtsReady = (status == TextToSpeech.SUCCESS)
//                    cont.resume(Unit)
//                }
//            }
//        }
        if (!isTtsReady) return
        suspendCancellableCoroutine { cont ->
            val utteranceId = "mqttAlert_${System.currentTimeMillis()}"
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) {
                    if (id == utteranceId) cont.resume(Unit)
                }

                @Deprecated("Still required by the abstract class on older APIs")
                override fun onError(id: String?) {
                    if (id == utteranceId) cont.resume(Unit)
                }

                override fun onError(id: String?, errorCode: Int) {
                    if (id == utteranceId) cont.resume(Unit)
                }
            })

            val params = Bundle()
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (result == TextToSpeech.ERROR) {
                initTts()
                cont.resume(Unit)
            }
        }
    }
}