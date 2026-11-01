package com.yidongtong.guardian

data class SelectedNotificationContent(
    val sender: String,
    val body: String,
)

object NotificationContentSelector {
    fun select(
        appName: String,
        title: String,
        text: String,
        bigText: String,
        textLines: List<String>,
    ): SelectedNotificationContent? {
        val body = sequenceOf(
            bigText,
            text,
            textLines.filter { it.isNotBlank() }.joinToString("\n"),
        ).map(MessageSanitizer::clean).firstOrNull { it.isNotBlank() } ?: return null
        val sender = MessageSanitizer.clean(title)
            .ifBlank { MessageSanitizer.clean(appName) }
            .ifBlank { "未知应用" }
            .take(80)
        return SelectedNotificationContent(sender, body)
    }
}
