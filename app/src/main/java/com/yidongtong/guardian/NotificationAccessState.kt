package com.yidongtong.guardian

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService

object NotificationAccessState {
    private const val REBIND_THROTTLE_MS = 30_000L
    private var lastRebindRequestAt = 0L

    fun isEnabled(context: Context): Boolean = containsComponent(
        enabledComponents = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty(),
        packageName = context.packageName,
        className = IncomingNotificationListenerService::class.java.name,
    )

    fun requestRebindIfEnabled(context: Context) {
        if (!isEnabled(context)) return
        val now = SystemClock.elapsedRealtime()
        if (lastRebindRequestAt != 0L && now - lastRebindRequestAt < REBIND_THROTTLE_MS) return
        lastRebindRequestAt = now
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(context, IncomingNotificationListenerService::class.java),
            )
        }
    }

    fun containsComponent(
        enabledComponents: String,
        packageName: String,
        className: String,
    ): Boolean = enabledComponents.split(':').any { flattened ->
        val separator = flattened.indexOf('/')
        if (separator <= 0 || separator == flattened.lastIndex) return@any false
        val enabledPackage = flattened.substring(0, separator)
        val rawClassName = flattened.substring(separator + 1)
        val enabledClassName = if (rawClassName.startsWith('.')) enabledPackage + rawClassName else rawClassName
        enabledPackage == packageName && enabledClassName == className
    }
}
