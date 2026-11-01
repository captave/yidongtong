package com.yidongtong.guardian

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 开机或应用升级后恢复后台守护服务，避免短信提醒在重启后失效。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                MessageGuardService.start(context)
                NotificationAccessState.requestRebindIfEnabled(context)
            }
        }
    }
}
