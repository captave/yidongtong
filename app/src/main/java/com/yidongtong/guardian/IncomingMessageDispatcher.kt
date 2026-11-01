package com.yidongtong.guardian

import android.content.Context
import android.os.SystemClock

object IncomingMessageDispatcher {
    private val deduplicator = RecentMessageDeduplicator(windowMillis = 10_000L)

    fun deliver(context: Context, message: IncomingMessage, source: String = "unknown") {
        if (!deduplicator.shouldDeliver(message.body, SystemClock.elapsedRealtime())) return
        val notificationId = MessageNotification.nextMessageId()
        LatestMessageStore.publishTransient(message)
        MessageNotification.show(context, message, notificationId)
        runCatching { MessageAnalysisCoordinator.process(context, message, source, notificationId) }
    }
}
