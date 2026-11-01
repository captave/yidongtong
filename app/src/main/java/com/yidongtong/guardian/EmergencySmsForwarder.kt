package com.yidongtong.guardian

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager

object EmergencySmsForwarder {
    fun send(context: Context, message: IncomingMessage, reason: String, recordId: Long): Boolean {
        if (!UserPreferences(context).emergencyForwardingEnabled) return false
        if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return false
        val contact = EmergencyContactStore(context).read() ?: return false
        val text = EmergencyForwardMessageBuilder.build(message.sender, message.body, reason)
        val database = MessageHistoryDatabase.get(context)
        return try {
            val manager = context.getSystemService(SmsManager::class.java)
            val parts = manager.divideMessage(text)
            if (!database.markSending(recordId, parts.size)) return false
            val sentIntents = ArrayList<PendingIntent>(parts.size)
            parts.indices.forEach { index ->
                val intent = Intent(context, SmsSentReceiver::class.java).apply {
                    action = SmsSentReceiver.ACTION_SMS_SENT
                    data = Uri.Builder()
                        .scheme("yidongtong")
                        .authority("sms-sent")
                        .appendPath(recordId.toString())
                        .appendPath(index.toString())
                        .build()
                    putExtra(SmsSentReceiver.EXTRA_RECORD_ID, recordId)
                    putExtra(SmsSentReceiver.EXTRA_PART_INDEX, index)
                }
                sentIntents += PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
            if (parts.size > 1) {
                manager.sendMultipartTextMessage(contact.number, null, parts, sentIntents, null)
            } else {
                manager.sendTextMessage(contact.number, null, text, sentIntents.single(), null)
            }
            true
        } catch (_: SecurityException) {
            database.markSmsFailed(recordId)
            false
        } catch (_: RuntimeException) {
            database.markSmsFailed(recordId)
            false
        }
    }
}
