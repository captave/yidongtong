package com.yidongtong.guardian

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyForwardMessageBuilderTest {
    @Test
    fun forwardedWarningContainsSenderReasonAndBoundedBody() {
        val text = EmergencyForwardMessageBuilder.build(
            sender = "10690000",
            body = "请立即转账".repeat(100),
            reason = "疑似冒充客服诱导转账",
        )

        assertTrue(text.contains("易懂通危险提醒"))
        assertTrue(text.contains("10690000"))
        assertTrue(text.contains("疑似冒充客服诱导转账"))
        assertTrue(text.length <= 420)
        assertFalse(text.contains("API"))
    }
}
