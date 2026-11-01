package com.yidongtong.guardian

import java.util.concurrent.atomic.AtomicLong

object LatestMessageStore {
    @Volatile
    var latest: IncomingMessage? = null
        private set

    private val versionCounter = AtomicLong(0L)
    val version: Long get() = versionCounter.get()

    private val listeners = java.util.concurrent.CopyOnWriteArraySet<(IncomingMessage) -> Unit>()
    @Volatile private var latestRecordId: Long = -1L

    fun publishTransient(message: IncomingMessage) {
        latest = message
        versionCounter.incrementAndGet()
        listeners.forEach { it(message) }
    }

    @Synchronized
    fun bindRecord(recordId: Long, message: IncomingMessage) {
        if (recordId < latestRecordId) return
        latestRecordId = recordId
        publishTransient(message)
    }

    fun publish(recordId: Long, message: IncomingMessage) = bindRecord(recordId, message)

    fun addListener(listener: (IncomingMessage) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (IncomingMessage) -> Unit) {
        listeners.remove(listener)
    }
}
