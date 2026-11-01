package com.yidongtong.guardian

object MessageRiskDetector {
    private val sixDigits = Regex("\\p{Nd}{6}")

    fun isRisky(message: String): Boolean =
        message.contains("验证码", ignoreCase = true) || sixDigits.containsMatchIn(message)
}
