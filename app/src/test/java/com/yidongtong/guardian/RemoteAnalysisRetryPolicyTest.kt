package com.yidongtong.guardian

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteAnalysisRetryPolicyTest {
    @Test
    fun `network failures retry at most twice`() {
        assertTrue(RemoteAnalysisRetryPolicy.shouldRetry(runAttemptCount = 0))
        assertTrue(RemoteAnalysisRetryPolicy.shouldRetry(runAttemptCount = 1))
        assertFalse(RemoteAnalysisRetryPolicy.shouldRetry(runAttemptCount = 2))
    }
}
