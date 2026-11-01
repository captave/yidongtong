package com.yidongtong.guardian

object DeepSeekApiKeyValidator {
    fun normalize(value: String): String = value.trim().take(MAX_LENGTH)

    fun isValid(value: String): Boolean = KEY_PATTERN.matches(normalize(value))

    private val KEY_PATTERN = Regex("sk-[A-Za-z0-9_-]{20,237}")
    private const val MAX_LENGTH = 240
}
