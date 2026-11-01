package com.yidongtong.guardian

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.os.Bundle

class LauncherActivity : Activity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeContext.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
        )
        finish()
    }
}
