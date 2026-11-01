package com.yidongtong.guardian

object SpeechTextBuilder {
    private const val SAFETY_WARNING =
        "安全提醒。验证码不要告诉陌生人，不要点击陌生链接，也不要向陌生账户转账。"

    fun build(sender: String, body: String, risky: Boolean): String {
        val message = "来自 $sender 的短信。$body"
        return if (risky) SAFETY_WARNING + message else message
    }
}
