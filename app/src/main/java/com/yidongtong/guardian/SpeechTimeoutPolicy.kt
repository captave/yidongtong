package com.yidongtong.guardian

object SpeechTimeoutPolicy {
    private const val MAX_ESTIMATED_CHARACTER_COUNT = 2_500
    private const val MILLIS_PER_CHARACTER = 450L
    private const val BUFFER_MILLIS = 30_000L

    fun forCharacterCount(characterCount: Int): Long =
        characterCount
            .coerceIn(0, MAX_ESTIMATED_CHARACTER_COUNT)
            .toLong()
            .times(MILLIS_PER_CHARACTER)
            .plus(BUFFER_MILLIS)
            .coerceAtLeast(BUFFER_MILLIS)
}
