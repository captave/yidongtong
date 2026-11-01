package com.yidongtong.guardian

object ForwardContentRedactor {
    fun redact(body: String): String {
        // Exclude entire credential-bearing sentences, including alphabetic codes and unusual lengths.
        val sentences = body.split(Regex("(?<=[。！？!?\\n])"))
        return sentences.joinToString("") { sentence ->
            if (Regex("验证码|校验码|验证代码|密码|口令|OTP|取件码|提货码|领取码|兑换码|登录代码|确认码", RegexOption.IGNORE_CASE)
                    .containsMatchIn(sentence)) "[含验证码或口令的内容已隐藏]。" else sentence
        }.replace(Regex("(?i)(?:[a-z][a-z0-9+.-]*://|www\\.)\\S+|(?:[a-z0-9-]+\\.)+(?:com|cn|net|org|xyz|top|vip|info|io|app|shop|cc|co|me)(?:/\\S*)?"), "[网址已隐藏]")
            .replace(Regex("\\d{12,}"), "[长号码已隐藏]")
    }
}
