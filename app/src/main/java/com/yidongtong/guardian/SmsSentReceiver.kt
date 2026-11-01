package com.yidongtong.guardian

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SMS_SENT) return
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        val partIndex = intent.getIntExtra(EXTRA_PART_INDEX, -1)
        if (recordId <= 0L || partIndex < 0) return
        MessageHistoryDatabase.get(context).recordSmsPartResult(
            recordId,
            partIndex,
            success = resultCode == Activity.RESULT_OK,
        )
    }

    companion object {
        const val ACTION_SMS_SENT = "com.yidongtong.guardian.action.SMS_SENT"
        const val EXTRA_RECORD_ID = "record_id"
        const val EXTRA_PART_INDEX = "part_index"
    }
}
