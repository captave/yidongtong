package com.yidongtong.guardian

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Telephony
import java.text.Collator
import java.util.Locale

data class NotificationAppInfo(
    val packageName: String,
    val label: String,
    val isDefaultSms: Boolean,
    val icon: Drawable?,
)

object NotificationAppCatalog {
    fun load(context: Context): List<NotificationAppInfo> {
        val packageManager = context.packageManager
        val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
        val packages = LinkedHashMap<String, Boolean>()

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, 0).forEach { resolved ->
            val packageName = resolved.activityInfo.packageName
            if (packageName != context.packageName) {
                packages[packageName] = packageName == defaultSmsPackage
            }
        }
        if (!defaultSmsPackage.isNullOrBlank() && defaultSmsPackage != context.packageName) {
            packages[defaultSmsPackage] = true
        }
        NotificationCapturePreferences(context).selectedPackages().forEach { packageName ->
            if (packageName == context.packageName) return@forEach
            val installed = runCatching { applicationInfo(packageManager, packageName) }.getOrNull() != null
            if (installed) packages.putIfAbsent(packageName, packageName == defaultSmsPackage)
        }

        val collator = Collator.getInstance(Locale.CHINA)
        return packages.keys.mapNotNull { packageName ->
            val info = runCatching { applicationInfo(packageManager, packageName) }.getOrNull()
                ?: return@mapNotNull null
            NotificationAppInfo(
                packageName = packageName,
                label = packageManager.getApplicationLabel(info).toString().ifBlank { packageName },
                isDefaultSms = packageName == defaultSmsPackage,
                icon = runCatching { packageManager.getApplicationIcon(info) }.getOrNull(),
            )
        }.sortedWith { first, second -> collator.compare(first.label, second.label) }
    }

    private fun applicationInfo(
        packageManager: PackageManager,
        packageName: String,
    ): android.content.pm.ApplicationInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
}
