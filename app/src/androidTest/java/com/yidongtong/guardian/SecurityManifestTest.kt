package com.yidongtong.guardian

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityManifestTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun sensitivePermissionsMatchRequestedFeatures() {
        val permissions = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            .toSet()

        assertTrue(permissions.contains(Manifest.permission.RECEIVE_SMS))
        assertTrue(permissions.contains(Manifest.permission.SEND_SMS))
        assertTrue(permissions.contains(Manifest.permission.INTERNET))
        assertFalse(permissions.contains(Manifest.permission.CALL_PHONE))
        assertFalse(permissions.contains(Manifest.permission.READ_CONTACTS))
        assertFalse(permissions.contains(Manifest.permission.READ_SMS))
    }

    @Test
    fun privateComponentsAreNotExported() {
        val packageManager = context.packageManager
        val mainActivity = packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0)
        val historyActivity = packageManager.getActivityInfo(ComponentName(context, HistoryActivity::class.java), 0)
        val speechService = packageManager.getServiceInfo(ComponentName(context, SpeechService::class.java), 0)
        val smsSentReceiver = packageManager.getReceiverInfo(ComponentName(context, SmsSentReceiver::class.java), 0)

        assertFalse(mainActivity.exported)
        assertFalse(historyActivity.exported)
        assertFalse(speechService.exported)
        assertFalse(smsSentReceiver.exported)
    }

    @Test
    fun smsReceiverRequiresSystemBroadcastPermission() {
        val receiver = context.packageManager.getReceiverInfo(ComponentName(context, SmsReceiver::class.java), 0)

        assertEquals(Manifest.permission.BROADCAST_SMS, receiver.permission)
    }

    @Test
    fun notificationListenerRequiresSystemBindingPermission() {
        val service = context.packageManager.getServiceInfo(
            ComponentName(context, IncomingNotificationListenerService::class.java),
            0,
        )

        assertEquals(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE, service.permission)
    }

    @Test
    fun apiKeyIsEncryptedAtRestAndCanBeDeleted() {
        val store = ApiKeyStore(context)
        val key = "sk-abcdefghijklmnopqrstuvwxyz123456"
        store.clear()

        assertTrue(store.save(key))
        assertEquals(key, store.read())
        val rawPreferences = context.getSharedPreferences("encrypted_api_credentials", Context.MODE_PRIVATE)
            .all.values.joinToString("|")
        assertFalse(rawPreferences.contains(key))

        store.clear()
        assertFalse(store.hasKey())
    }

    @Test
    fun unsavedApiKeyIsExcludedFromActivityState() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.findViewById<android.widget.EditText>(R.id.apiKeyInput).isSaveEnabled)
            }
        }
    }
}
