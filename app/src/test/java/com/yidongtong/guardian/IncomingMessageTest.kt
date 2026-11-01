package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingMessageTest {
    @Test
    fun riskAfterDisplayLimit_isStillDetected() {
        val rawBody = "普通内容".repeat(500) + "验证码 123456"

        val message = IncomingMessage.fromRaw("10690000", rawBody)

        assertEquals(MessageSanitizer.MAX_LENGTH, message.body.length)
        assertTrue(message.risky)
    }

    @Test
    fun rawMessageProcessing_hasAnOverallBound() {
        val message = IncomingMessage.fromRaw("10690000", "文".repeat(50_000) + "验证码")

        assertEquals(MessageSanitizer.MAX_LENGTH, message.body.length)
    }
}
