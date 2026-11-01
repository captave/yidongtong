package com.yidongtong.guardian

enum class RiskLevel {
    SAFE,
    SUSPICIOUS,
    DANGEROUS,
}

data class RiskAssessment(
    val level: RiskLevel,
    val reason: String,
) {
    val requiresEmergencyAlert: Boolean
        get() = level == RiskLevel.DANGEROUS
}

object DeepSeekRiskResponseParser {
    fun parse(content: String): RiskAssessment? {
        val values = StrictJsonObjectParser(content).parse() ?: return null
        if (values.keys != setOf("level", "reason")) return null
        val level = when (values["level"]) {
            "safe" -> RiskLevel.SAFE
            "suspicious" -> RiskLevel.SUSPICIOUS
            "dangerous" -> RiskLevel.DANGEROUS
            else -> return null
        }
        val reason = values["reason"]
            ?.let(MessageSanitizer::clean)
            ?.take(MAX_REASON_LENGTH)
            ?.ifBlank { null }
            ?: return null
        return RiskAssessment(level, reason)
    }

    private const val MAX_REASON_LENGTH = 160
}

private class StrictJsonObjectParser(private val input: String) {
    private var index = 0

    fun parse(): Map<String, String>? {
        skipWhitespace()
        if (!take('{')) return null
        val result = linkedMapOf<String, String>()
        repeat(2) { fieldIndex ->
            skipWhitespace()
            val key = readString() ?: return null
            if (result.containsKey(key)) return null
            skipWhitespace()
            if (!take(':')) return null
            skipWhitespace()
            val value = readString() ?: return null
            result[key] = value
            skipWhitespace()
            if (fieldIndex == 0 && !take(',')) return null
        }
        skipWhitespace()
        if (!take('}')) return null
        skipWhitespace()
        return result.takeIf { index == input.length }
    }

    private fun readString(): String? {
        if (!take('"')) return null
        val output = StringBuilder()
        while (index < input.length) {
            val current = input[index++]
            when {
                current == '"' -> return output.toString()
                current.code in 0x00..0x1F -> return null
                current != '\\' -> output.append(current)
                index >= input.length -> return null
                else -> when (val escaped = input[index++]) {
                    '"', '\\', '/' -> output.append(escaped)
                    'b' -> output.append('\b')
                    'f' -> output.append('\u000C')
                    'n' -> output.append('\n')
                    'r' -> output.append('\r')
                    't' -> output.append('\t')
                    'u' -> {
                        if (index + 4 > input.length) return null
                        val value = input.substring(index, index + 4).toIntOrNull(16) ?: return null
                        output.append(value.toChar())
                        index += 4
                    }
                    else -> return null
                }
            }
        }
        return null
    }

    private fun skipWhitespace() {
        while (index < input.length && input[index] in JSON_WHITESPACE) index++
    }

    private fun take(expected: Char): Boolean {
        if (index >= input.length || input[index] != expected) return false
        index++
        return true
    }

    private companion object {
        val JSON_WHITESPACE = charArrayOf(' ', '\t', '\n', '\r')
    }
}
