package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechTextBuilderTest {
    @Test
    fun normalMessage_readsSenderAndBody() {
        assertEquals(
            "来自 10690000 的短信。明天下午社区活动。",
            SpeechTextBuilder.build("10690000", "明天下午社区活动。", risky = false),
        )
    }

    @Test
    fun riskyMessage_readsSafetyWarningFirst() {
        assertEquals(
            "安全提醒。验证码不要告诉陌生人，不要点击陌生链接，也不要向陌生账户转账。来自 银行 的短信。验证码 123456。",
            SpeechTextBuilder.build("银行", "验证码 123456。", risky = true),
        )
    }
}
