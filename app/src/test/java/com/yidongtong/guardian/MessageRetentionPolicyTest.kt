package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageRetentionPolicyTest {
    @Test
    fun cutoffIsExactlyNinetyDaysBeforeNow() {
        val now = 1_800_000_000_000L

        assertEquals(now - 90L * 24 * 60 * 60 * 1000, MessageRetentionPolicy.cutoff(now))
    }
}
