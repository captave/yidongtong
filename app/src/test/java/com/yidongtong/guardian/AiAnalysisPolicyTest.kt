package com.yidongtong.guardian

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAnalysisPolicyTest {
    @Test
    fun `analysis requires both user switch and configured credential`() {
        assertTrue(AiAnalysisPolicy.shouldAnalyze(enabledByUser = true, credentialConfigured = true))
        assertFalse(AiAnalysisPolicy.shouldAnalyze(enabledByUser = false, credentialConfigured = true))
        assertFalse(AiAnalysisPolicy.shouldAnalyze(enabledByUser = true, credentialConfigured = false))
    }
}
