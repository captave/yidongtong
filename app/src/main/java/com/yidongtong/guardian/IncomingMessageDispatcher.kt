package com.yidongtong.guardian

import android.content.Context
import android.os.SystemClock

object IncomingMessageDispatcher {
    private val deduplicator = RecentMessageDeduplicator(windowMillis = 10_000L)

    fun deliver(context: Context, message: IncomingMessage, source: String = "unknown") {
        if (!deduplicator.shouldDeliver(message.body, SystemClock.elapsedRealtime())) return
        val assessment = RiskRuleLibrary.assess(message.body, RiskRulePreferences(context).disabled())
        val classified = message.copy(risky = assessment.level != RiskLevel.SAFE,
            riskLevel = assessment.level, riskReason = assessment.reason)
        val notificationId = MessageNotification.nextMessageId()
        LatestMessageStore.publishTransient(classified)
        MessageNotification.show(context, classified, notificationId)
        runCatching { MessageAnalysisCoordinator.process(context, classified, source, notificationId) }
    }
}
