package com.yidongtong.guardian

object MessageSanitizer {
    const val MAX_LENGTH = 2_000

    fun clean(input: String, maxLength: Int = MAX_LENGTH): String = buildString(minOf(input.length, maxLength)) {
        input.forEach { character ->
            if (length >= maxLength) return@buildString
            when {
                character == '\n' -> append(character)
                character == '\r' -> Unit
                !character.isISOControl() -> append(character)
            }
        }
    }.trim()
}
