package com.group5.roammate.ui.pet

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.group5.roammate.pet.PetDialogueLanguage
import java.util.Locale

enum class PetVoicePhase { Ready, Listening, Processing, Speaking }

/**
 * Main-thread, one-utterance voice adapter. The owning dialog calls resume/pause with its lifecycle.
 * Offline recognition is preferred. System recognition is never selected without the user's switch.
 * Engines and microphone sessions are destroyed when the dialog/activity stops being resumed.
 */
class PetVoiceController(context: Context, private val onUtterance: (String) -> Unit) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var resumed = false
    private var recognizer: SpeechRecognizer? = null
    private var speech: TextToSpeech? = null
    private var speechReady = false
    private var engineGeneration = 0
    private var listeningGeneration = 0
    private var speakingGeneration = 0
    private var timeout: Runnable? = null

    var phase by mutableStateOf(PetVoicePhase.Ready)
        private set
    var partialText by mutableStateOf("")
        private set
    var notice by mutableStateOf("")
        private set
    var language by mutableStateOf(PetDialogueLanguage.English)
        private set
    var offlineRecognitionAvailable by mutableStateOf(false)
        private set
    var systemRecognitionAvailable by mutableStateOf(false)
        private set

    fun resume() {
        if (resumed) return
        resumed = true
        offlineRecognitionAvailable = Build.VERSION.SDK_INT >= 31 && runCatching {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
        }.getOrDefault(false)
        systemRecognitionAvailable = runCatching { SpeechRecognizer.isRecognitionAvailable(appContext) }.getOrDefault(false)
        initializeSpeech()
    }

    fun changeLanguage(value: PetDialogueLanguage) {
        stop()
        language = value
        notice = ""
    }

    fun permissionDenied() {
        notice = language.text("Microphone access is off. You can still type a message.", "未获得麦克风权限，你仍然可以输入文字。")
    }

    fun startListening(useSystemRecognizer: Boolean) {
        if (!resumed) return
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionDenied()
            return
        }
        stop()
        val canRecognize = if (useSystemRecognizer) systemRecognitionAvailable else offlineRecognitionAvailable
        if (!canRecognize) {
            notice = language.text(
                "This recognition service isn't installed. Type a message, or choose system speech if available.",
                "当前识别服务不可用。可以输入文字，或选用已安装的系统语音服务。",
            )
            return
        }
        notice = ""
        partialText = ""
        phase = PetVoicePhase.Listening
        val generation = ++listeningGeneration
        // Let the previous spoken reply fall silent before opening the microphone.
        main.postDelayed({
            if (!resumed || generation != listeningGeneration) return@postDelayed
            try {
                val active = if (!useSystemRecognizer && Build.VERSION.SDK_INT >= 31) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(appContext)
                }
                recognizer = active
                active.setRecognitionListener(listener(generation))
                val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.tag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    if (!useSystemRecognizer) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
                active.startListening(request)
                timeout = Runnable {
                    if (generation == listeningGeneration) {
                        releaseRecognizer()
                        phase = PetVoicePhase.Ready
                        notice = language.text("Listening timed out. Please try a short sentence.", "聆听已超时，请重新说一句简短的话。")
                    }
                }.also { main.postDelayed(it, 25_000L) }
            } catch (_: Exception) {
                releaseRecognizer()
                phase = PetVoicePhase.Ready
                notice = language.text("Voice recognition couldn't start. You can type instead.", "语音识别暂时无法启动，可以改用文字。")
            }
        }, 250L)
    }

    fun finishListening() {
        if (phase != PetVoicePhase.Listening) return
        val active = recognizer
        if (active == null) {
            stop()
        } else {
            phase = PetVoicePhase.Processing
            runCatching { active.stopListening() }.onFailure { stop() }
        }
    }

    fun speak(text: String) {
        if (!resumed) return
        releaseRecognizer()
        stopSpeaking()
        val engine = speech
        if (!speechReady || engine == null) {
            notice = language.text("Read the reply below; the phone's speech engine isn't ready.", "手机朗读服务尚未就绪，可以先阅读回复。")
            return
        }
        val locale = Locale.forLanguageTag(language.tag)
        // Never silently use a cloud-only or not-yet-installed TTS voice.
        val voice = runCatching {
            engine.voices.orEmpty()
                .filter { it.locale.language == locale.language && !it.isNetworkConnectionRequired &&
                    !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
                .sortedWith(compareByDescending<android.speech.tts.Voice> { it.locale.country == locale.country }
                    .thenByDescending { it.quality }.thenBy { it.name })
                .firstOrNull()
        }.getOrNull()
        if (voice == null || runCatching { engine.setVoice(voice) }.getOrDefault(TextToSpeech.ERROR) != TextToSpeech.SUCCESS) {
            notice = language.text("No installed offline voice for this language. The reply is available as text.", "手机没有安装这个语言的离线语音包，回复仍可用文字查看。")
            return
        }
        val utteranceId = "buddy-${++speakingGeneration}"
        notice = ""
        phase = PetVoicePhase.Speaking
        val result = runCatching {
            engine.setSpeechRate(0.95f)
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }.getOrDefault(TextToSpeech.ERROR)
        if (result != TextToSpeech.SUCCESS) {
            phase = PetVoicePhase.Ready
            notice = language.text("Audio isn't available right now; read the reply below.", "暂时无法朗读，请阅读下方回复。")
        }
    }

    fun stop() {
        releaseRecognizer()
        stopSpeaking()
        partialText = ""
        phase = PetVoicePhase.Ready
    }

    fun pause() {
        resumed = false
        stop()
        ++engineGeneration
        speechReady = false
        runCatching { speech?.shutdown() }
        speech = null
        main.removeCallbacksAndMessages(null)
    }

    private fun initializeSpeech() {
        val generation = ++engineGeneration
        try {
            speech = TextToSpeech(appContext) { status ->
                main.post {
                    if (!resumed || generation != engineGeneration) return@post
                    speechReady = status == TextToSpeech.SUCCESS
                    if (speechReady) speech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit
                        override fun onDone(utteranceId: String?) = finishSpeech(utteranceId, false)
                        @Deprecated("Required for older TTS engines")
                        override fun onError(utteranceId: String?) = finishSpeech(utteranceId, true)
                        override fun onError(utteranceId: String?, errorCode: Int) = finishSpeech(utteranceId, true)
                    })
                }
            }
        } catch (_: Exception) {
            speechReady = false
        }
    }

    private fun finishSpeech(utteranceId: String?, failed: Boolean) {
        main.post {
            if (!resumed || utteranceId != "buddy-$speakingGeneration") return@post
            phase = PetVoicePhase.Ready
            if (failed) notice = language.text("Audio stopped. You can read Buddy's reply.", "朗读已停止，可以阅读 Buddy 的回复。")
        }
    }

    private fun stopSpeaking() {
        ++speakingGeneration
        runCatching { speech?.stop() }
        if (phase == PetVoicePhase.Speaking) phase = PetVoicePhase.Ready
    }

    private fun releaseRecognizer() {
        ++listeningGeneration
        timeout?.let(main::removeCallbacks)
        timeout = null
        val active = recognizer
        recognizer = null
        runCatching { active?.cancel() }
        runCatching { active?.destroy() }
    }

    private fun listener(generation: Int): RecognitionListener = object : RecognitionListener {
        private fun valid() = resumed && generation == listeningGeneration
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() { if (valid()) phase = PetVoicePhase.Processing }
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
        override fun onPartialResults(partialResults: Bundle?) {
            if (valid()) partialText = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        }
        override fun onResults(results: Bundle?) {
            if (!valid()) return
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
            releaseRecognizer()
            phase = PetVoicePhase.Ready
            partialText = ""
            if (text.isNotEmpty()) onUtterance(text) else notice = language.text("I didn't catch that. Please try again or type.", "没听清，可以再试一次或输入文字。")
        }
        override fun onError(error: Int) {
            if (!valid()) return
            releaseRecognizer()
            phase = PetVoicePhase.Ready
            partialText = ""
            notice = when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> language.text("Microphone access is needed for speech. Typing still works.", "语音需要麦克风权限，仍然可以输入文字。")
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> language.text("I didn't catch that. Try a short sentence or type below.", "没听清，试着说一句简短的话，或在下面输入。")
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> language.text("This speech service has no model for the selected language. Switch language or type.", "当前识别服务没有所选语言的语音模型，可以切换语言或输入文字。")
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> language.text("The selected system service needs a working connection. You can type instead.", "所选系统识别服务需要网络连接，可以改用文字。")
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> language.text("The microphone is busy. Wait a moment and try again.", "麦克风正在使用中，请稍后重试。")
                else -> language.text("Speech isn't available right now. Please try again or type.", "语音暂时不可用，请重试或输入文字。")
            }
        }
    }
}
