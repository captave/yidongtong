package com.yidongtong.guardian

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageHistoryDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun recordsOlderThanNinetyDaysAreDeleted() {
        val database = MessageHistoryDatabase.get(context)
        database.clearAll()
        val now = 1_800_000_000_000L
        val message = IncomingMessage.fromRaw("10086", "普通通知")
        val assessment = RiskAssessment(RiskLevel.SAFE, "普通通知")

        database.insert(message, assessment, "test", MessageRetentionPolicy.cutoff(now) - 1)
        database.insert(message, assessment, "test", now)

        assertEquals(1, database.recent(20, now).size)
        database.clearAll()
    }

    @Test
    fun insertedRecordCanBeLoadedForBackgroundAnalysis() {
        val database = MessageHistoryDatabase.get(context)
        database.clearAll()
        val message = IncomingMessage.fromRaw("95588", "账户通知")
        val id = database.insert(message, RiskAssessment(RiskLevel.SAFE, "等待分析"), "test")

        val record = database.find(id)

        assertNotNull(record)
        assertEquals("95588", record?.sender)
        assertEquals("账户通知", record?.body)
        database.clearAll()
    }

    @Test
    fun expiredRecordCannotBeLoadedByBackgroundAnalysis() {
        val database = MessageHistoryDatabase.get(context)
        database.clearAll()
        val now = 1_800_000_000_000L
        val id = database.insert(
            IncomingMessage.fromRaw("10086", "过期通知"),
            RiskAssessment(RiskLevel.SAFE, "普通通知"),
            "test",
            MessageRetentionPolicy.cutoff(now) - 1,
        )

        assertNull(database.find(id, now))
        database.clearAll()
    }

    @Test
    fun duplicateSmsPartCallbackDoesNotCompleteAnotherPart() {
        val database = MessageHistoryDatabase.get(context)
        database.clearAll()
        val id = database.insert(
            IncomingMessage.fromRaw("10086", "危险测试"),
            RiskAssessment(RiskLevel.DANGEROUS, "测试"),
            "sms",
        )
        database.markSending(id, totalParts = 2)

        database.recordSmsPartResult(id, partIndex = 0, success = true)
        database.recordSmsPartResult(id, partIndex = 0, success = true)
        assertEquals(SmsDeliveryState.SENDING, database.find(id)?.deliveryState)

        database.recordSmsPartResult(id, partIndex = 1, success = true)
        assertEquals(SmsDeliveryState.SENT, database.find(id)?.deliveryState)
        database.clearAll()
    }

    @Test
    fun historyCanBeSearchedAndDeletedById() {
        val database = MessageHistoryDatabase.get(context)
        database.clearAll()
        val keepId = database.insert(
            IncomingMessage.fromRaw("银行", "工资到账通知"),
            RiskAssessment(RiskLevel.SAFE, "普通通知"),
            "sms",
        )
        val deleteId = database.insert(
            IncomingMessage.fromRaw("陌生号码", "验证码 123456"),
            RiskAssessment(RiskLevel.SUSPICIOUS, "包含验证码"),
            "sms",
        )

        assertEquals(listOf(deleteId), database.search("验证码").map { it.id })
        assertEquals(true, database.delete(deleteId))
        assertNull(database.find(deleteId))
        assertNotNull(database.find(keepId))
        database.clearAll()
    }
}
