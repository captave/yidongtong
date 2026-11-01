package com.yidongtong.guardian

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentMessageDeduplicatorTest {
    @Test
    fun sameMessageWithinWindowIsSuppressed() {
        val deduplicator = RecentMessageDeduplicator(windowMillis = 10_000)

        assertTrue(deduplicator.shouldDeliver("验证码 666666", nowMillis = 1_000))
        assertFalse(deduplicator.shouldDeliver("验证码 666666", nowMillis = 5_000))
    }

    @Test
    fun changedOrExpiredMessageIsDelivered() {
        val deduplicator = RecentMessageDeduplicator(windowMillis = 10_000)

        assertTrue(deduplicator.shouldDeliver("第一条", nowMillis = 1_000))
        assertTrue(deduplicator.shouldDeliver("第二条", nowMillis = 2_000))
        assertTrue(deduplicator.shouldDeliver("第一条", nowMillis = 12_000))
    }
}
