package com.yidongtong.guardian

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isEmpty()) return

        val rawBody = buildString {
            for (part in parts) {
                val remaining = IncomingMessage.MAX_SCAN_LENGTH - length
                if (remaining <= 0) break
                append(part.displayMessageBody.orEmpty().take(remaining))
            }
        }
        if (rawBody.isBlank()) return

        val message = IncomingMessage.fromRaw(
            sender = parts.first().displayOriginatingAddress.orEmpty().take(MAX_SENDER_LENGTH),
            rawBody = rawBody,
        )
        val pendingResult = goAsync()
        try {
            executor.execute {
                try {
                    IncomingMessageDispatcher.deliver(context.applicationContext, message, source = "sms")
                } finally {
                    pendingResult.finish()
                }
            }
        } catch (_: RuntimeException) {
            val notificationId = MessageNotification.nextMessageId()
            LatestMessageStore.publishTransient(message)
            MessageNotification.show(context, message, notificationId)
            pendingResult.finish()
        }
    }

    private companion object {
        const val MAX_SENDER_LENGTH = 80
        val executor = ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(16),
        )
    }
}
