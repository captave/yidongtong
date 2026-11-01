package com.yidongtong.guardian

object HistorySearchTerms {
    private const val MAX_QUERY_LENGTH = 80

    fun asLikePattern(query: String): String {
        val clean = MessageSanitizer.clean(query).take(MAX_QUERY_LENGTH)
        val escaped = buildString(clean.length) {
            clean.forEach { character ->
                if (character == '%' || character == '_' || character == '\\') append('\\')
                append(character)
            }
        }
        return "%$escaped%"
    }
}
