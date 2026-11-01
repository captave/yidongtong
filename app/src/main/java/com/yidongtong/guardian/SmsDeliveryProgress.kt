package com.yidongtong.guardian

enum class SmsDeliveryState {
    NONE,
    SENDING,
    SENT,
    FAILED,
}

data class SmsDeliveryProgress(
    val state: SmsDeliveryState,
    val successfulParts: Int,
    val totalParts: Int,
) {
    fun record(success: Boolean): SmsDeliveryProgress {
        if (state != SmsDeliveryState.SENDING) return this
        if (!success) return copy(state = SmsDeliveryState.FAILED)
        val completed = (successfulParts + 1).coerceAtMost(totalParts)
        return copy(
            state = if (completed >= totalParts) SmsDeliveryState.SENT else SmsDeliveryState.SENDING,
            successfulParts = completed,
        )
    }
}
