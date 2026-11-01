package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekApiKeyValidatorTest {
    @Test
    fun trimsAndAcceptsOnlyReasonableDeepSeekKeys() {
        val key = "  sk-abcdefghijklmnopqrstuvwxyz123456  "

        assertTrue(DeepSeekApiKeyValidator.isValid(key))
        assertEquals("sk-abcdefghijklmnopqrstuvwxyz123456", DeepSeekApiKeyValidator.normalize(key))
        assertFalse(DeepSeekApiKeyValidator.isValid(""))
        assertFalse(DeepSeekApiKeyValidator.isValid("not-a-key"))
        assertFalse(DeepSeekApiKeyValidator.isValid("sk-short"))
        assertFalse(DeepSeekApiKeyValidator.isValid("sk-valid-looking-key-with spaces"))
    }
}
