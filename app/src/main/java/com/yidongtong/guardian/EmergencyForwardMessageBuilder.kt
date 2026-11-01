package com.yidongtong.guardian

object EmergencyForwardMessageBuilder {
    fun build(sender: String, body: String, reason: String): String {
        val safeSender = ForwardContentRedactor.redact(MessageSanitizer.clean(sender)).take(80).ifBlank { "未知号码" }
        val safeReason = ForwardContentRedactor.redact(MessageSanitizer.clean(reason)).take(120).ifBlank { "疑似诈骗信息" }
        val prefix = "【易懂通危险提醒】家人的手机收到可疑或危险信息，需核实。来源：$safeSender。判断：$safeReason。内容："
        val suffix = "。请及时联系家人核实，勿转账、勿透露验证码。"
        val available = (MAX_LENGTH - prefix.length - suffix.length).coerceAtLeast(0)
        return prefix + ForwardContentRedactor.redact(MessageSanitizer.clean(body)).take(available) + suffix
    }

    private const val MAX_LENGTH = 420
}
