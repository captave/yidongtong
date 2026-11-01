package com.yidongtong.guardian

import android.app.Notification
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class IncomingNotificationListenerService : NotificationListenerService() {
    private val handler = Handler(Looper.getMainLooper())
    private val capturePreferences by lazy { NotificationCapturePreferences(this) }

    @Volatile
    private var defaultSmsPackage: String? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this)
        Log.i(TAG, "notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "notification listener disconnected, rebind scheduled")
        // 系统或厂商省电策略可能断开监听；仍有授权时主动重连，避免后台漏读。
        handler.postDelayed({
            if (NotificationAccessState.isEnabled(this)) {
                NotificationAccessState.requestRebindIfEnabled(this)
            }
        }, REBIND_DELAY_MS)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification?) {
        val posted = statusBarNotification ?: return
        if (posted.packageName == packageName) return
        val smsPackage = defaultSmsPackage ?: Telephony.Sms.getDefaultSmsPackage(this).also {
            defaultSmsPackage = it
        }
        if (!NotificationCapturePolicy.shouldCapture(
                posted.packageName,
                capturePreferences.selectedPackages(),
                packageName,
                smsPackage,
            )
        ) {
            return
        }
        val notification = posted.notification ?: return
        if (notification.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_GROUP_SUMMARY) != 0) return

        val extras = notification.extras ?: Bundle.EMPTY
        val appName = runCatching {
            val applicationInfo = packageManager.getApplicationInfo(posted.packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrDefault(posted.packageName)
        val content = NotificationContentSelector.select(
            appName = appName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty(),
            textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.map(CharSequence::toString)
                .orEmpty(),
        ) ?: return
        if (content.body.startsWith("【易懂通危险提醒】")) return

        IncomingMessageDispatcher.deliver(
            this,
            IncomingMessage.fromRaw(content.sender, content.body),
            source = "notification",
        )
    }

    companion object {
        private const val TAG = "IncomingListener"
        private const val REBIND_DELAY_MS = 2_000L

        @Volatile
        private var instance: IncomingNotificationListenerService? = null

        /** 通过系统绑定状态判断真实连接，避免连接标记过期导致后台不再重连。 */
        fun isConnected(): Boolean {
            val service = instance ?: return false
            return runCatching {
                service.currentInterruptionFilter != INTERRUPTION_FILTER_UNKNOWN
            }.getOrDefault(false)
        }
    }
}