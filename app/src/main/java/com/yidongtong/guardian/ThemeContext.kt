package com.yidongtong.guardian

import android.content.Context
import android.content.res.Configuration

object ThemeContext {
    fun wrap(base: Context): Context {
        val dark = UserPreferences(base).darkTheme
        val configuration = Configuration(base.resources.configuration)
        configuration.uiMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or
            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        return base.createConfigurationContext(configuration)
    }
}
