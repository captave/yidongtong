package com.yidongtong.guardian

import android.content.Context

class UserPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var darkTheme: Boolean
        get() = preferences.getBoolean(KEY_DARK_THEME, false)
        set(value) = preferences.edit().putBoolean(KEY_DARK_THEME, value).apply()

    var speechRate: Float
        get() = SpeechRatePolicy.normalize(preferences.getFloat(KEY_SPEECH_RATE, SpeechRatePolicy.DEFAULT))
        set(value) = preferences.edit().putFloat(KEY_SPEECH_RATE, SpeechRatePolicy.normalize(value)).apply()

    var deepSeekEnabled: Boolean
        get() = preferences.getBoolean(KEY_DEEPSEEK_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_DEEPSEEK_ENABLED, value).apply()

    var emergencyForwardingEnabled: Boolean
        get() = preferences.getBoolean(KEY_FORWARDING_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_FORWARDING_ENABLED, value).apply()

    var sendSmsDenialCount: Int
        get() = preferences.getInt(KEY_SEND_SMS_DENIAL_COUNT, 0)
        set(value) = preferences.edit().putInt(KEY_SEND_SMS_DENIAL_COUNT, value.coerceAtLeast(0)).apply()

    private companion object {
        const val NAME = "user_preferences"
        const val KEY_DARK_THEME = "dark_theme"
        const val KEY_SPEECH_RATE = "speech_rate"
        const val KEY_DEEPSEEK_ENABLED = "deepseek_enabled"
        const val KEY_FORWARDING_ENABLED = "emergency_forwarding_enabled"
        const val KEY_SEND_SMS_DENIAL_COUNT = "send_sms_denial_count"
    }
}
