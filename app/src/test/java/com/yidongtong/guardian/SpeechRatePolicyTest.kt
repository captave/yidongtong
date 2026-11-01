package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechRatePolicyTest {
    @Test
    fun rateIsClampedToAccessibleRange() {
        assertEquals(0.60f, SpeechRatePolicy.normalize(0.1f), 0.001f)
        assertEquals(0.85f, SpeechRatePolicy.normalize(0.85f), 0.001f)
        assertEquals(1.20f, SpeechRatePolicy.normalize(2.0f), 0.001f)
    }
}
