package com.yidongtong.guardian

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageRiskDetectorTest {
    @Test
    fun keywordVerificationCode_isRisky() {
        assertTrue(MessageRiskDetector.isRisky("您的验证码为 84A9，请妥善保管"))
    }

    @Test
    fun sixConsecutiveDigits_areRisky() {
        assertTrue(MessageRiskDetector.isRisky("本次登录代码是 381926，十分钟内有效"))
    }

    @Test
    fun sixFullWidthDigits_areRisky() {
        assertTrue(MessageRiskDetector.isRisky("动态口令：１２３４５６"))
    }

    @Test
    fun fewerThanSixDigits_withoutKeyword_isNotRisky() {
        assertFalse(MessageRiskDetector.isRisky("您本月已使用 12345 MB 流量"))
    }

    @Test
    fun ordinaryMessage_isNotRisky() {
        assertFalse(MessageRiskDetector.isRisky("明天下午三点在社区活动室见面。"))
    }
}
