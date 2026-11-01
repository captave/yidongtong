package com.yidongtong.guardian

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class SpeechService : Service(), TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = null
    private var ready = false
    private var currentRequest: SpeechRequest? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeoutAction: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        textToSpeech = TextToSpeech(this, this).apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = finishRequest(utteranceId)
                @Deprecated("Deprecated in Android")
                override fun onError(utteranceId: String?) = failRequest(utteranceId)
                override fun onError(utteranceId: String?, errorCode: Int) = failRequest(utteranceId)
                override fun onStop(utteranceId: String?, interrupted: Boolean) = finishRequest(utteranceId)
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopReading()
            return START_NOT_STICKY
        }
        val sender = MessageSanitizer.clean(intent?.getStringExtra(MessageContract.EXTRA_SENDER).orEmpty())
            .ifBlank { "未知号码" }
            .take(80)
        val body = MessageSanitizer.clean(intent?.getStringExtra(MessageContract.EXTRA_BODY).orEmpty())
        val risky = intent?.getBooleanExtra(MessageContract.EXTRA_RISKY, false) == true
        if (body.isBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, speakingNotification())
        // Latest tap wins: a new request intentionally replaces any current reading.
        currentRequest = SpeechRequest(
            text = SpeechTextBuilder.build(sender, body, risky),
            startId = startId,
            utteranceId = "$UTTERANCE_PREFIX-$startId",
        )
        scheduleTimeout(startId, INITIALIZATION_TIMEOUT_MS, "语音服务启动超时，请稍后重试。")
        if (ready) speakPending()
        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            showSpeechProblem("普通话语音启动失败，请检查手机的文字转语音设置。")
            stopSelf()
            return
        }
        val engine = textToSpeech ?: return
        val languageStatus = engine.setLanguage(Locale.SIMPLIFIED_CHINESE)
        if (languageStatus == TextToSpeech.LANG_MISSING_DATA || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
            showSpeechProblem("手机缺少普通话语音包，请在系统的文字转语音设置中安装。")
            stopSelf()
            return
        }
        val offlineVoice = engine.voices.orEmpty()
            .asSequence()
            .filter { it.locale.language == Locale.CHINESE.language && !it.isNetworkConnectionRequired }
            .sortedWith(
                compareByDescending<android.speech.tts.Voice> { it.locale.country == Locale.CHINA.country }
                    .thenByDescending { it.quality },
            )
            .firstOrNull()
        if (offlineVoice == null || engine.setVoice(offlineVoice) == TextToSpeech.ERROR) {
            showSpeechProblem("为保护短信隐私，只使用离线普通话语音。请先在系统设置中安装离线语音包。")
            stopSelf()
            return
        }
        engine.setSpeechRate(UserPreferences(this).speechRate)
        engine.setPitch(1.0f)
        ready = true
        speakPending()
    }

    private fun speakPending() {
        val request = currentRequest ?: return
        cancelTimeout()
        val engine = textToSpeech
        engine?.setSpeechRate(UserPreferences(this).speechRate)
        val result = engine?.speak(
            request.text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            request.utteranceId,
        )
        if (result == TextToSpeech.ERROR) {
            showSpeechProblem("暂时无法朗读，请检查手机音量和文字转语音设置。")
            stopSelfResult(request.startId)
            currentRequest = null
        } else {
            val timeout = SpeechTimeoutPolicy.forCharacterCount(request.text.length)
            scheduleTimeout(request.startId, timeout, "朗读时间过长，已自动停止。")
        }
    }

    private fun finishRequest(utteranceId: String?) {
        mainHandler.post {
            val request = currentRequest ?: return@post
            if (utteranceId != request.utteranceId) return@post
            cancelTimeout()
            currentRequest = null
            stopSelfResult(request.startId)
        }
    }

    private fun failRequest(utteranceId: String?) {
        mainHandler.post {
            val request = currentRequest ?: return@post
            if (utteranceId != request.utteranceId) return@post
            showSpeechProblem("朗读意外中断，请稍后重试。")
            cancelTimeout()
            currentRequest = null
            stopSelfResult(request.startId)
        }
    }

    private fun scheduleTimeout(startId: Int, delayMillis: Long, message: String) {
        cancelTimeout()
        timeoutAction = Runnable {
            val request = currentRequest
            if (request != null && request.startId == startId) {
                showSpeechProblem(message)
                textToSpeech?.stop()
                currentRequest = null
                stopSelfResult(startId)
            }
        }.also { mainHandler.postDelayed(it, delayMillis) }
    }

    private fun cancelTimeout() {
        timeoutAction?.let(mainHandler::removeCallbacks)
        timeoutAction = null
    }

    private fun stopReading() {
        cancelTimeout()
        currentRequest = null
        textToSpeech?.stop()
        stopSelf()
    }

    override fun onDestroy() {
        cancelTimeout()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_SPEECH,
            "语音朗读",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "朗读短信时显示"
            setSound(null, null)
        }
        val errorChannel = NotificationChannel(
            CHANNEL_SPEECH_ERRORS,
            "语音问题提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "普通话语音不可用或朗读失败时显示"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(channel, errorChannel))
    }

    private fun speakingNotification(): Notification {
        val stopIntent = Intent(this, SpeechService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = android.app.PendingIntent.getService(
            this,
            0,
            stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_SPEECH)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("易懂通正在朗读")
            .setContentText("可以随时停止朗读")
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "停止朗读", stopPendingIntent).build())
            .build()
    }

    private fun showSpeechProblem(message: String) {
        val notification = Notification.Builder(this, CHANNEL_SPEECH_ERRORS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("无法朗读短信")
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(ERROR_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_SPEECH = "speech_playback"
        private const val CHANNEL_SPEECH_ERRORS = "speech_errors"
        private const val NOTIFICATION_ID = 40_101
        private const val ERROR_NOTIFICATION_ID = 40_102
        private const val UTTERANCE_PREFIX = "incoming-message"
        private const val ACTION_SPEAK = "com.yidongtong.guardian.action.SPEAK"
        private const val ACTION_STOP = "com.yidongtong.guardian.action.STOP_SPEECH"
        private const val INITIALIZATION_TIMEOUT_MS = 15_000L

        fun intent(context: Context, message: IncomingMessage): Intent =
            Intent(context, SpeechService::class.java).apply {
                action = ACTION_SPEAK
                putExtra(MessageContract.EXTRA_SENDER, message.sender)
                putExtra(MessageContract.EXTRA_BODY, message.body)
                putExtra(MessageContract.EXTRA_RISKY, message.risky)
            }

        fun start(context: Context, message: IncomingMessage) {
            context.startForegroundService(intent(context, message))
        }
    }

    private data class SpeechRequest(
        val text: String,
        val startId: Int,
        val utteranceId: String,
    )
}
