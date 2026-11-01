package com.yidongtong.guardian

object RemoteAnalysisRetryPolicy {
    fun shouldRetry(runAttemptCount: Int): Boolean = runAttemptCount < 2
}
