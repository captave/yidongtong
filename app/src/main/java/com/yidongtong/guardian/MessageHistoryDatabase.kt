package com.yidongtong.guardian

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class MessageRecord(
    val id: Long,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val assessment: RiskAssessment,
    val forwarded: Boolean,
    val source: String,
    val notificationId: Int,
    val deliveryState: SmsDeliveryState,
)

class MessageHistoryDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender TEXT NOT NULL,
                body TEXT NOT NULL,
                received_at INTEGER NOT NULL,
                risk_level TEXT NOT NULL,
                reason TEXT NOT NULL,
                source TEXT NOT NULL,
                notification_id INTEGER NOT NULL,
                delivery_state TEXT NOT NULL DEFAULT 'NONE',
                sent_parts INTEGER NOT NULL DEFAULT 0,
                total_parts INTEGER NOT NULL DEFAULT 0,
                forwarded INTEGER NOT NULL DEFAULT 0
            )""".trimIndent(),
        )
        database.execSQL("CREATE INDEX messages_received_at ON messages(received_at DESC)")
        createSmsPartTable(database)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            database.execSQL("ALTER TABLE messages ADD COLUMN notification_id INTEGER NOT NULL DEFAULT 100000")
        }
        if (oldVersion < 3) {
            database.execSQL("ALTER TABLE messages ADD COLUMN delivery_state TEXT NOT NULL DEFAULT 'NONE'")
            database.execSQL("ALTER TABLE messages ADD COLUMN sent_parts INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE messages ADD COLUMN total_parts INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 4) createSmsPartTable(database)
    }

    @Synchronized
    fun insert(
        message: IncomingMessage,
        assessment: RiskAssessment,
        source: String,
        now: Long = System.currentTimeMillis(),
        notificationId: Int = MessageNotification.nextMessageId(),
    ): Long {
        deleteExpired(now)
        val values = ContentValues().apply {
            put("sender", message.sender.take(80))
            put("body", message.body.take(MessageSanitizer.MAX_LENGTH))
            put("received_at", now)
            put("risk_level", assessment.level.name)
            put("reason", assessment.reason.take(160))
            put("source", source.take(40))
            put("notification_id", notificationId)
        }
        return writableDatabase.insertOrThrow("messages", null, values)
    }

    @Synchronized
    fun updateAssessment(id: Long, assessment: RiskAssessment) {
        val values = ContentValues().apply {
            put("risk_level", assessment.level.name)
            put("reason", assessment.reason.take(160))
        }
        writableDatabase.update("messages", values, "id = ?", arrayOf(id.toString()))
    }

    @Synchronized
    fun markSending(id: Long, totalParts: Int): Boolean {
        if (totalParts <= 0) return false
        val current = find(id) ?: return false
        if (current.deliveryState != SmsDeliveryState.NONE) return false
        val values = ContentValues().apply {
            put("delivery_state", SmsDeliveryState.SENDING.name)
            put("sent_parts", 0)
            put("total_parts", totalParts)
        }
        writableDatabase.delete("sms_send_parts", "record_id = ?", arrayOf(id.toString()))
        return writableDatabase.update("messages", values, "id = ?", arrayOf(id.toString())) == 1
    }

    @Synchronized
    fun recordSmsPartResult(id: Long, partIndex: Int, success: Boolean) {
        val database = writableDatabase
        val totals = database.query(
            "messages",
            arrayOf("delivery_state", "total_parts"),
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) return
            Pair(
                runCatching { SmsDeliveryState.valueOf(cursor.getString(0)) }
                    .getOrDefault(SmsDeliveryState.FAILED),
                cursor.getInt(1),
            )
        }
        if (totals.first != SmsDeliveryState.SENDING || partIndex !in 0 until totals.second) return
        database.insertWithOnConflict(
            "sms_send_parts",
            null,
            ContentValues().apply {
                put("record_id", id)
                put("part_index", partIndex)
                put("success", if (success) 1 else 0)
            },
            SQLiteDatabase.CONFLICT_IGNORE,
        )
        val counts = database.rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(success), 0) FROM sms_send_parts WHERE record_id = ?",
            arrayOf(id.toString()),
        ).use { cursor ->
            cursor.moveToFirst()
            Pair(cursor.getInt(0), cursor.getInt(1))
        }
        val state = when {
            counts.first > counts.second -> SmsDeliveryState.FAILED
            counts.first >= totals.second -> SmsDeliveryState.SENT
            else -> SmsDeliveryState.SENDING
        }
        database.update(
            "messages",
            ContentValues().apply {
                put("delivery_state", state.name)
                put("sent_parts", counts.second)
                put("forwarded", if (state == SmsDeliveryState.SENT) 1 else 0)
            },
            "id = ?",
            arrayOf(id.toString()),
        )
    }

    @Synchronized
    fun markSmsFailed(id: Long) {
        writableDatabase.update(
            "messages",
            ContentValues().apply { put("delivery_state", SmsDeliveryState.FAILED.name) },
            "id = ?",
            arrayOf(id.toString()),
        )
    }

    @Synchronized
    fun find(id: Long, now: Long = System.currentTimeMillis()): MessageRecord? {
        deleteExpired(now)
        return readableDatabase.query(
            "messages",
            arrayOf("id", "sender", "body", "received_at", "risk_level", "reason", "forwarded", "source", "notification_id", "delivery_state"),
            "id = ? AND received_at >= ?",
            arrayOf(id.toString(), MessageRetentionPolicy.cutoff(now).toString()),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val level = runCatching { RiskLevel.valueOf(cursor.getString(4)) }.getOrDefault(RiskLevel.SAFE)
            MessageRecord(
                id = cursor.getLong(0),
                sender = cursor.getString(1),
                body = cursor.getString(2),
                receivedAt = cursor.getLong(3),
                assessment = RiskAssessment(level, cursor.getString(5)),
                forwarded = cursor.getInt(6) == 1,
                source = cursor.getString(7),
                notificationId = cursor.getInt(8),
                deliveryState = runCatching { SmsDeliveryState.valueOf(cursor.getString(9)) }
                    .getOrDefault(SmsDeliveryState.NONE),
            )
        }
    }

    @Synchronized
    fun recent(limit: Int = 20, now: Long = System.currentTimeMillis()): List<MessageRecord> {
        deleteExpired(now)
        val safeLimit = limit.coerceIn(1, 100)
        return readableDatabase.query(
            "messages",
            arrayOf("id", "sender", "body", "received_at", "risk_level", "reason", "forwarded", "source", "notification_id", "delivery_state"),
            null,
            null,
            null,
            null,
            "received_at DESC",
            safeLimit.toString(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val level = runCatching { RiskLevel.valueOf(cursor.getString(4)) }.getOrDefault(RiskLevel.SAFE)
                    add(
                        MessageRecord(
                            id = cursor.getLong(0),
                            sender = cursor.getString(1),
                            body = cursor.getString(2),
                            receivedAt = cursor.getLong(3),
                            assessment = RiskAssessment(level, cursor.getString(5)),
                            forwarded = cursor.getInt(6) == 1,
                            source = cursor.getString(7),
                            notificationId = cursor.getInt(8),
                            deliveryState = runCatching { SmsDeliveryState.valueOf(cursor.getString(9)) }
                                .getOrDefault(SmsDeliveryState.NONE),
                        ),
                    )
                }
            }
        }
    }

    @Synchronized
    fun search(query: String, limit: Int = 100, now: Long = System.currentTimeMillis()): List<MessageRecord> {
        if (query.isBlank()) return recent(limit, now)
        deleteExpired(now)
        val pattern = HistorySearchTerms.asLikePattern(query)
        return queryRecords(
            selection = "sender LIKE ? ESCAPE '\\' OR body LIKE ? ESCAPE '\\' OR reason LIKE ? ESCAPE '\\'",
            selectionArgs = arrayOf(pattern, pattern, pattern),
            limit = limit,
        )
    }

    @Synchronized
    fun delete(id: Long): Boolean {
        if (id <= 0L) return false
        val database = writableDatabase
        database.delete("sms_send_parts", "record_id = ?", arrayOf(id.toString()))
        return database.delete("messages", "id = ?", arrayOf(id.toString())) == 1
    }

    @Synchronized
    fun clearAll() {
        writableDatabase.delete("sms_send_parts", null, null)
        writableDatabase.delete("messages", null, null)
    }

    private fun queryRecords(
        selection: String?,
        selectionArgs: Array<String>?,
        limit: Int,
    ): List<MessageRecord> = readableDatabase.query(
        "messages",
        RECORD_COLUMNS,
        selection,
        selectionArgs,
        null,
        null,
        "received_at DESC",
        limit.coerceIn(1, 100).toString(),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.toMessageRecord())
        }
    }

    private fun android.database.Cursor.toMessageRecord(): MessageRecord {
        val level = runCatching { RiskLevel.valueOf(getString(4)) }.getOrDefault(RiskLevel.SAFE)
        return MessageRecord(
            id = getLong(0),
            sender = getString(1),
            body = getString(2),
            receivedAt = getLong(3),
            assessment = RiskAssessment(level, getString(5)),
            forwarded = getInt(6) == 1,
            source = getString(7),
            notificationId = getInt(8),
            deliveryState = runCatching { SmsDeliveryState.valueOf(getString(9)) }
                .getOrDefault(SmsDeliveryState.NONE),
        )
    }

    @Synchronized
    fun deleteExpiredRecords(now: Long = System.currentTimeMillis()): Int = deleteExpired(now)

    private fun deleteExpired(now: Long): Int {
        val database = writableDatabase
        val cutoff = MessageRetentionPolicy.cutoff(now).toString()
        database.execSQL(
            "DELETE FROM sms_send_parts WHERE record_id IN (SELECT id FROM messages WHERE received_at < ?)",
            arrayOf(cutoff),
        )
        return database.delete(
            "messages",
            "received_at < ?",
            arrayOf(cutoff),
        )
    }

    private fun createSmsPartTable(database: SQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS sms_send_parts (
                record_id INTEGER NOT NULL,
                part_index INTEGER NOT NULL,
                success INTEGER NOT NULL,
                PRIMARY KEY(record_id, part_index)
            )""".trimIndent(),
        )
    }

    companion object {
        private val RECORD_COLUMNS = arrayOf(
            "id", "sender", "body", "received_at", "risk_level", "reason", "forwarded", "source",
            "notification_id", "delivery_state",
        )
        private const val DATABASE_NAME = "message_history.db"
        private const val DATABASE_VERSION = 4
        @Volatile private var instance: MessageHistoryDatabase? = null

        fun get(context: Context): MessageHistoryDatabase = instance ?: synchronized(this) {
            instance ?: MessageHistoryDatabase(context).also { instance = it }
        }
    }
}
