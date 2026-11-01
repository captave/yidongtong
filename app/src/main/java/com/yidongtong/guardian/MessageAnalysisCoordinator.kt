package com.yidongtong.guardian

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object MessageAnalysisCoordinator {
    fun process(context: Context, message: IncomingMessage, source: String, notificationId: Int): Long {
        val appContext = context.applicationContext
        val localDecision = MessageProcessingDecision.combine(message.risky, null)
        val id = MessageHistoryDatabase.get(appContext)
            .insert(message, localDecision.assessment, source, notificationId = notificationId)
        LatestMessageStore.bindRecord(id, message)
        val preferences = UserPreferences(appContext)
        if (!AiAnalysisPolicy.shouldAnalyze(preferences.deepSeekEnabled, ApiKeyStore(appContext).hasKey())) return id

        val request = OneTimeWorkRequestBuilder<MessageAnalysisWorker>()
            .setInputData(Data.Builder().putLong(MessageAnalysisWorker.KEY_RECORD_ID, id).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "message-analysis-$id",
            ExistingWorkPolicy.KEEP,
            request,
        )
        return id
    }
}
