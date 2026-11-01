package com.yidongtong.guardian

import android.content.Context

data class EmergencyContact(val name: String, val number: String)

class EmergencyContactStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): EmergencyContact? {
        val number = preferences.getString(KEY_NUMBER, null)?.let(EmergencyNumber::normalize) ?: return null
        val name = preferences.getString(KEY_NAME, null).orEmpty().ifBlank { DEFAULT_NAME }
        return EmergencyContact(name = name, number = number)
    }

    fun save(nameInput: String, numberInput: String): Boolean {
        val number = EmergencyNumber.normalize(numberInput) ?: return false
        val name = MessageSanitizer.clean(nameInput).take(MAX_NAME_LENGTH).ifBlank { DEFAULT_NAME }
        preferences.edit().putString(KEY_NAME, name).putString(KEY_NUMBER, number).apply()
        return true
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "emergency_contact"
        const val KEY_NAME = "name"
        const val KEY_NUMBER = "number"
        const val MAX_NAME_LENGTH = 30
        const val DEFAULT_NAME = "家人"
    }
}
