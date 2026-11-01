package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageProcessingDecisionTest {
    @Test
    fun aiDangerousOverridesLocalSafeAndAllowsForwarding() {
        val decision = MessageProcessingDecision.combine(
            localRisky = false,
            remote = RiskAssessment(RiskLevel.DANGEROUS, "冒充公检法要求转账"),
        )

        assertEquals(RiskLevel.DANGEROUS, decision.level)
        assertTrue(decision.shouldForward)
    }

    @Test
    fun localCodeWarningIsSuspiciousWhenCloudIsUnavailable() {
        val decision = MessageProcessingDecision.combine(localRisky = true, remote = null)

        assertEquals(RiskLevel.SUSPICIOUS, decision.level)
        assertFalse(decision.shouldForward)
    }

    @Test
    fun remoteSafeDoesNotEraseLocalCodeWarning() {
        val decision = MessageProcessingDecision.combine(
            localRisky = true,
            remote = RiskAssessment(RiskLevel.SAFE, "普通验证码通知"),
        )

        assertEquals(RiskLevel.SUSPICIOUS, decision.level)
    }
}
