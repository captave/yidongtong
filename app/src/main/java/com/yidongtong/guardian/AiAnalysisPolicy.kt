package com.yidongtong.guardian

object AiAnalysisPolicy {
    fun shouldAnalyze(enabledByUser: Boolean, credentialConfigured: Boolean): Boolean =
        enabledByUser && credentialConfigured
}
