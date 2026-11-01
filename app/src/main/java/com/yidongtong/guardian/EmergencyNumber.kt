package com.yidongtong.guardian

object EmergencyNumber {
    private val separators = Regex("[\\s()－-]")
    private val safeNumber = Regex("\\+?\\d{3,20}")

    fun normalize(input: String): String? {
        val normalized = input.trim().replace(separators, "")
        return normalized.takeIf(safeNumber::matches)
    }
}
