package com.yidongtong.guardian

import android.content.Context

class RiskRulePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("risk_rules", Context.MODE_PRIVATE)
    fun disabled(): Set<String> = prefs.getStringSet("disabled", emptySet())?.toSet().orEmpty()
    fun setEnabled(id: String, enabled: Boolean) {
        val updated = disabled().toMutableSet()
        if (enabled) updated.remove(id) else updated.add(id)
        prefs.edit().putStringSet("disabled", updated).apply()
    }
    fun restoreDefaults() { prefs.edit().remove("disabled").apply() }
}
