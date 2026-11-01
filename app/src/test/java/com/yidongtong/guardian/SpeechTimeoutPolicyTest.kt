package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechTimeoutPolicyTest {
    @Test
    fun longAcceptedMessage_getsEnoughTimeForEstimatedPlayback() {
        assertEquals(930_000L, SpeechTimeoutPolicy.forCharacterCount(2_000))
    }

    @Test
    fun unexpectedOversizedInput_isStillBounded() {
        assertEquals(1_155_000L, SpeechTimeoutPolicy.forCharacterCount(50_000))
    }
}
