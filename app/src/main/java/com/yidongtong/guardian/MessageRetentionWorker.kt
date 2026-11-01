package com.yidongtong.guardian

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class MessageRetentionWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result = runCatching {
        MessageHistoryDatabase.get(applicationContext).deleteExpiredRecords()
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        private const val PERIODIC_WORK_NAME = "message-retention-cleanup"

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.enqueueUniqueWork(
                "$PERIODIC_WORK_NAME-startup",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<MessageRetentionWorker>().build(),
            )
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<MessageRetentionWorker>(1, TimeUnit.DAYS).build(),
            )
        }
    }
}
