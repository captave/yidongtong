package com.yidongtong.guardian

object MessageRetentionPolicy {
    private const val RETENTION_DAYS = 90L
    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    fun cutoff(nowMillis: Long): Long = nowMillis - RETENTION_DAYS * MILLIS_PER_DAY
}
