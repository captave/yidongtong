package com.yidongtong.guardian

import org.junit.Assert.*
import org.junit.Test

class RiskRuleLibraryTest {
    @Test fun strictTriggersAreDangerousRegardlessOfLengthOrNegation() {
        listOf("验证码1234", "OTP A7B9", "取件码A-258", "验证码已发送", "不要向陌生人转账",
            "不要点击链接", "访问example.cn", "https://example.com", "汇款已到账").forEach {
            assertEquals(it, RiskLevel.DANGEROUS, RiskRuleLibrary.assess(it).level)
        }
    }
    @Test fun ordinaryMessagesAndLongOrderNumbersRemainUnflagged() {
        listOf("明天下午社区见面", "订单2026102912345678已发货", "尾号1234账户到账200元").forEach {
            assertEquals(it, RiskLevel.SAFE, RiskRuleLibrary.assess(it).level)
        }
    }
    @Test fun everyRuleHasAnExampleItActuallyMatches() {
        RiskRuleLibrary.rules.forEach { assertTrue(it.id, it.pattern.containsMatchIn(it.example)) }
        assertEquals(24, RiskRuleLibrary.rules.map { it.id }.toSet().size)
    }
    @Test fun disabledRuleDoesNotTrigger() {
        assertEquals(RiskLevel.SAFE, RiskRuleLibrary.assess("验证码1234", setOf("D01")).level)
    }
    @Test fun aiCannotLowerDangerButCanRaiseSuspicious() {
        val local = RiskRuleLibrary.assess("验证码1234")
        assertEquals(RiskLevel.DANGEROUS, MessageProcessingDecision.combine(local, RiskAssessment(RiskLevel.SAFE, "普通信息")).level)
        assertEquals(RiskLevel.DANGEROUS, MessageProcessingDecision.combine(RiskRuleLibrary.assess("爸我换号码了"), RiskAssessment(RiskLevel.DANGEROUS, "诱骗")).level)
    }
    @Test fun forwardedCredentialsAndLinksAreHidden() {
        val result = ForwardContentRedactor.redact("验证码 A7B9。访问https://example.com确认。银行卡6222000012345678。")
        assertFalse(result.contains("A7B9")); assertFalse(result.contains("https://")); assertFalse(result.contains("6222000012345678"))
    }
}
