package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskAssessmentTest {
    @Test
    fun dangerousAssessmentRequiresEmergencyAlert() {
        val assessment = RiskAssessment(RiskLevel.DANGEROUS, "诱导转账")

        assertTrue(assessment.requiresEmergencyAlert)
    }

    @Test
    fun suspiciousAssessmentDoesNotAutomaticallyMessageContact() {
        val assessment = RiskAssessment(RiskLevel.SUSPICIOUS, "内容需要核实")

        assertFalse(assessment.requiresEmergencyAlert)
    }

    @Test
    fun parserAcceptsOnlyKnownLevelsAndBoundsReason() {
        val longReason = "险".repeat(240)
        val parsed = DeepSeekRiskResponseParser.parse(
            """{"level":"dangerous","reason":"$longReason"}""",
        )

        assertEquals(RiskLevel.DANGEROUS, parsed?.level)
        assertEquals(160, parsed?.reason?.length)
        assertEquals(null, DeepSeekRiskResponseParser.parse("""{"level":"critical","reason":"x"}"""))
    }

    @Test
    fun parserRejectsAnythingExceptOneExactTwoFieldObject() {
        val valid = """{"reason":"疑似诱导转账","level":"dangerous"}"""
        assertEquals(RiskLevel.DANGEROUS, DeepSeekRiskResponseParser.parse(valid)?.level)

        val invalid = listOf(
            "说明如下：{\"level\":\"dangerous\",\"reason\":\"诱导转账\"}",
            "{\"level\":\"dangerous\",\"reason\":\"诱导转账\"} trailing",
            "{\"level\":\"dangerous\",\"reason\":\"诱导转账\",\"extra\":true}",
            "{\"level\":\"safe\",\"level\":\"dangerous\",\"reason\":\"重复字段\"}",
            "{\"level\":7,\"reason\":\"错误类型\"}",
            "[{\"level\":\"dangerous\",\"reason\":\"数组\"}]",
            "{\"level\":\"dangerous\",\"reason\":{\"text\":\"嵌套\"}}",
            "{\"level\":\"dangerous\",\"reason\":\"原始\u0001控制符\"}",
            "{\u000C\"level\":\"dangerous\",\"reason\":\"非法空白\"}",
        )
        invalid.forEach { assertEquals(null, DeepSeekRiskResponseParser.parse(it)) }
    }
}
