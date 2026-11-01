package com.yidongtong.guardian

class RecentMessageDeduplicator(
    private val windowMillis: Long,
) {
    private val deliveredAt = LinkedHashMap<String, Long>()

    @Synchronized
    fun shouldDeliver(body: String, nowMillis: Long): Boolean {
        deliveredAt.entries.removeAll { nowMillis - it.value >= windowMillis }
        val previous = deliveredAt[body]
        if (previous != null && nowMillis - previous < windowMillis) return false
        deliveredAt[body] = nowMillis
        while (deliveredAt.size > MAX_RECENT_MESSAGES) {
            deliveredAt.remove(deliveredAt.keys.first())
        }
        return true
    }

    private companion object {
        const val MAX_RECENT_MESSAGES = 50
    }
}
