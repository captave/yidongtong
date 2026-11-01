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
        val remote = DeepSeekRiskClient().analyze(apiKey ?: return Result.success(), record.sender, record.body)
            ?: return if (RemoteAnalysisRetryPolicy.shouldRetry(runAttemptCount)) Result.retry() else Result.success()
        val hadLocalWarning = record.assessment.level != RiskLevel.SAFE
        val finalDecision = MessageProcessingDecision.combine(hadLocalWarning, remote)
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
