package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmergencyNumberTest {
    @Test
    fun spacesHyphensAndParentheses_areNormalized() {
        assertEquals("+8613812345678", EmergencyNumber.normalize("+86 138-1234-5678"))
        assertEquals("01012345678", EmergencyNumber.normalize("(010) 1234 5678"))
    }

    @Test
    fun intentLikeOrUssdInput_isRejected() {
        assertNull(EmergencyNumber.normalize("tel:110"))
        assertNull(EmergencyNumber.normalize("*#06#"))
        assertNull(EmergencyNumber.normalize("13812345678;123"))
    }

    @Test
    fun emptyOrUnreasonablyLongInput_isRejected() {
        assertNull(EmergencyNumber.normalize(""))
        assertNull(EmergencyNumber.normalize("123456789012345678901"))
    }
}
