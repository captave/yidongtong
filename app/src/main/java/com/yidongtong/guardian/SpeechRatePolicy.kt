package com.yidongtong.guardian

object SpeechRatePolicy {
    const val DEFAULT = 0.82f
    const val MIN = 0.60f
    const val MAX = 1.20f

    fun normalize(value: Float): Float = value.coerceIn(MIN, MAX)
}
