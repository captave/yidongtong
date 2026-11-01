package com.yidongtong.guardian

import android.content.Context
import android.provider.Telephony

class NotificationCapturePreferences(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun selectedPackages(): Set<String> {
        val stored = preferences.getStringSet(KEY_SELECTED, null)
        if (stored != null) {
            return NotificationCapturePolicy.normalize(stored, appContext.packageName)
        }
        val defaults = defaultPackages()
        preferences.edit().putStringSet(KEY_SELECTED, defaults).apply()
        return defaults
    }

    fun setSelectedPackages(packages: Set<String>) {
        preferences.edit()
            .putStringSet(KEY_SELECTED, NotificationCapturePolicy.normalize(packages, appContext.packageName))
            .apply()
    }

    fun resetToDefault() = setSelectedPackages(defaultPackages())

    fun defaultPackages(): Set<String> = NotificationCapturePolicy.defaultPackages(
        defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(appContext),
        ownPackage = appContext.packageName,
    )

    private companion object {
        const val NAME = "notification_capture"
        const val KEY_SELECTED = "selected_packages"
    }
}
