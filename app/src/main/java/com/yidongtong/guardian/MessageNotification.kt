package com.yidongtong.guardian

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import java.util.concurrent.atomic.AtomicInteger

object MessageNotification {
    private const val CHANNEL_MESSAGES = "incoming_messages"
    private const val CHANNEL_NAME = "短信提醒"

    fun initialize(context: Context) {
        ensureChannel(context.getSystemService(NotificationManager::class.java))
    }

    fun nextMessageId(): Int = nextNotificationId.updateAndGet { current ->
        if (current >= 0x0FFFFFFE) 100_000 else current + 1
    }

    fun show(context: Context, message: IncomingMessage, notificationId: Int = nextMessageId()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        ensureChannel(manager)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MessageContract.EXTRA_SENDER, message.sender)
            putExtra(MessageContract.EXTRA_BODY, message.body)
            putExtra(MessageContract.EXTRA_RISKY, message.risky)
            putExtra(MessageContract.EXTRA_RISK_LEVEL, message.riskLevel.name)
            putExtra(MessageContract.EXTRA_RISK_REASON, message.riskReason)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val speechIntent = SpeechService.intent(context, message)
        val speechPendingIntent = PendingIntent.getForegroundService(
            context,
            notificationId xor 0x1357,
            speechIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when (message.riskLevel) {
            RiskLevel.DANGEROUS -> "⚠ 高危信息，请立即联系家人"
            RiskLevel.SUSPICIOUS -> "⚠ 信息需要核实，请警惕诈骗"
            RiskLevel.SAFE -> "收到新信息"
        }
        val expandedText = if (message.riskReason.isNotBlank()) {
            "${message.body}\n\n安全判断：${message.riskReason}"
        } else {
            message.body
        }
        val builder = Notification.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(context.getColor(if (message.riskLevel != RiskLevel.SAFE) R.color.warning_red else R.color.brand_blue))
            .setContentTitle(title)
            .setContentText(message.body)
            .setStyle(Notification.BigTextStyle().bigText(expandedText))
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(Notification.Action.Builder(null, "🔊 朗读内容", speechPendingIntent).build())

        if (message.riskLevel != RiskLevel.SAFE) {
            val contact = EmergencyContactStore(context).read()
            val dialerAvailable = contact?.let {
                Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", it.number, null))
                    .resolveActivity(context.packageManager) != null
            } == true
            if (contact != null && dialerAvailable) {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", contact.number, null))
                val dialPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId xor 0x2468,
                    dialIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                builder.addAction(Notification.Action.Builder(null, "☎ 联系${contact.name}", dialPendingIntent).build())
            } else {
                val setupIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra(MessageContract.EXTRA_FOCUS_CONTACT, true)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val setupPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId xor 0x369A,
                    setupIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val actionTitle = if (contact == null) "☎ 设置紧急联系人" else "☎ 查看紧急联系人"
                builder.addAction(Notification.Action.Builder(null, actionTitle, setupPendingIntent).build())
            }
        }

        val privateVersion = Notification.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("易懂通收到一条信息")
            .setContentText("解锁手机后查看内容")
            .build()
        builder.setPublicVersion(privateVersion)
        manager.notify(notificationId, builder.build())
    }

    private fun ensureChannel(manager: NotificationManager) {
        val channel = NotificationChannel(CHANNEL_MESSAGES, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "收到短信时显示大字提醒，并提供普通话朗读按钮"
            enableVibration(true)
            lightColor = Color.BLUE
            enableLights(true)
        }
        manager.createNotificationChannel(channel)
    }

    private val nextNotificationId = AtomicInteger(
        100_000 + (System.currentTimeMillis() and 0x00FFFFFF).toInt(),
    )
}
