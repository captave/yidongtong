package com.yidongtong.guardian

import android.app.Application

class YiDongTongApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationAccessState.requestRebindIfEnabled(this)
        MessageRetentionWorker.schedule(this)
    }
}
