package com.yidongtong.guardian

data class IncomingMessage(
    val sender: String,
    val body: String,
    val risky: Boolean = MessageRiskDetector.isRisky(body),
    val riskLevel: RiskLevel = if (risky) RiskLevel.SUSPICIOUS else RiskLevel.SAFE,
    val riskReason: String = if (risky) "包含验证码字样或连续六位数字" else "",
) {
    companion object {
        const val MAX_SCAN_LENGTH = 20_000

        fun fromRaw(sender: String, rawBody: String): IncomingMessage {
            val boundedBody = MessageSanitizer.clean(rawBody, MAX_SCAN_LENGTH)
            return IncomingMessage(
                sender = MessageSanitizer.clean(sender).ifBlank { "未知号码" }.take(80),
                body = boundedBody.take(MessageSanitizer.MAX_LENGTH).trim(),
                risky = MessageRiskDetector.isRisky(boundedBody),
            )
        }
    }
}
