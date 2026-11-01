package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsDeliveryProgressTest {
    @Test
    fun `all successful parts become sent`() {
        val first = SmsDeliveryProgress(SmsDeliveryState.SENDING, successfulParts = 0, totalParts = 2)
            .record(success = true)
        assertEquals(SmsDeliveryState.SENDING, first.state)
        assertEquals(SmsDeliveryState.SENT, first.record(success = true).state)
    }

    @Test
    fun `any failed part becomes failed and stays failed`() {
        val failed = SmsDeliveryProgress(SmsDeliveryState.SENDING, 0, 2).record(success = false)
        assertEquals(SmsDeliveryState.FAILED, failed.state)
        assertEquals(SmsDeliveryState.FAILED, failed.record(success = true).state)
    }
}
