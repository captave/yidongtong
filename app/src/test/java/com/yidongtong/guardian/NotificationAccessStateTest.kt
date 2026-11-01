package com.yidongtong.guardian

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAccessStateTest {
    @Test
    fun exactListenerComponentIsRecognized() {
        val enabled = "other.app/.Listener:com.yidongtong.guardian/.IncomingNotificationListenerService"

        assertTrue(
            NotificationAccessState.containsComponent(
                enabledComponents = enabled,
                packageName = "com.yidongtong.guardian",
                className = "com.yidongtong.guardian.IncomingNotificationListenerService",
            ),
        )
    }

    @Test
    fun similarPackageNameDoesNotCountAsEnabled() {
        assertFalse(
            NotificationAccessState.containsComponent(
                enabledComponents = "com.yidongtong.guardian.fake/.IncomingNotificationListenerService",
                packageName = "com.yidongtong.guardian",
                className = "com.yidongtong.guardian.IncomingNotificationListenerService",
            ),
        )
    }
}
