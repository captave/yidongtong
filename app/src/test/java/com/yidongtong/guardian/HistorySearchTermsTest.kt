package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Test

class HistorySearchTermsTest {
    @Test
    fun `like wildcard characters are escaped as literal search text`() {
        assertEquals("%50\\%\\_完成%", HistorySearchTerms.asLikePattern(" 50%_完成 "))
    }

    @Test
    fun `query is sanitized and length bounded`() {
        val raw = "查".repeat(120) + "\u0001"
        assertEquals(80 + 2, HistorySearchTerms.asLikePattern(raw).length)
    }
}
