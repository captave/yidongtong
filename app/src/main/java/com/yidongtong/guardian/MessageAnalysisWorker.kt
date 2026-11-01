package com.yidongtong.guardian

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class MessageAnalysisWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val recordId = inputData.getLong(KEY_RECORD_ID, -1L)
        if (recordId <= 0L) return Result.failure()

        val preferences = UserPreferences(applicationContext)
        val apiKey = ApiKeyStore(applicationContext).read()
        if (!AiAnalysisPolicy.shouldAnalyze(preferences.deepSeekEnabled, apiKey != null)) return Result.success()
        val database = MessageHistoryDatabase.get(applicationContext)
        val record = database.find(recordId) ?: return Result.success()
        val client = DeepSeekRiskClient()
        val remote = client.analyze(apiKey ?: return Result.success(), record.sender, record.body)
        // Re-check consent after the network request, before updating or sending anything.
        if (!preferences.deepSeekEnabled || !ApiKeyStore(applicationContext).hasKey()) return Result.success()
        if (remote == null) {
            val failed = RiskAssessment(record.assessment.level, client.failureReason)
            database.updateAssessment(recordId, failed)
            LatestMessageStore.publish(recordId, IncomingMessage(record.sender, record.body,
                failed.level != RiskLevel.SAFE, failed.level, failed.reason))
            return if (RemoteAnalysisRetryPolicy.shouldRetry(runAttemptCount)) Result.retry() else Result.success()
        }
        val local = RiskRuleLibrary.assess(record.body, RiskRulePreferences(applicationContext).disabled())
        val finalDecision = MessageProcessingDecision.combine(local, remote)
        database.updateAssessment(recordId, finalDecision.assessment)
        val assessedMessage = IncomingMessage(
            sender = record.sender,
            body = record.body,
            risky = finalDecision.assessment.level != RiskLevel.SAFE,
            riskLevel = finalDecision.assessment.level,
            riskReason = finalDecision.assessment.reason,
        )
        LatestMessageStore.publish(recordId, assessedMessage)
        MessageNotification.show(applicationContext, assessedMessage, record.notificationId)
        if (!finalDecision.shouldForward || record.deliveryState != SmsDeliveryState.NONE || record.source != "sms") {
            return Result.success()
        }

        if (preferences.emergencyForwardingEnabled) {
            EmergencySmsForwarder.send(
                applicationContext,
                assessedMessage,
                finalDecision.assessment.reason,
                recordId,
            )
        }
        return Result.success()
    }

    companion object {
        const val KEY_RECORD_ID = "record_id"
    }
}
