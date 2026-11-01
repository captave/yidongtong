package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MessageSanitizerTest {
    @Test
    fun controlCharacters_areRemovedButLineBreaksRemain() {
        assertEquals("第一行\n第二行", MessageSanitizer.clean("第一\u0000行\r\n第二\u0007行"))
    }

    @Test
    fun oversizedMessage_isBounded() {
        val result = MessageSanitizer.clean("验".repeat(5_000))

        assertEquals(MessageSanitizer.MAX_LENGTH, result.length)
        assertFalse(result.contains('\u0000'))
    }
}
