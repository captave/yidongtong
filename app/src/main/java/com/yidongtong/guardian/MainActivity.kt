package com.yidongtong.guardian

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.*
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private lateinit var contactStore: EmergencyContactStore
    private lateinit var preferences: UserPreferences
    private lateinit var apiKeyStore: ApiKeyStore
    private lateinit var mainScroll: ScrollView
    private lateinit var contactCard: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button
    private lateinit var notificationAccessText: TextView
    private lateinit var notificationAccessButton: Button
    private lateinit var notificationAppsButton: Button
    private lateinit var notificationAppsSummary: TextView
    private lateinit var senderText: TextView
    private lateinit var messageText: TextView
    private lateinit var riskPanel: LinearLayout
    private lateinit var riskHeadingText: TextView
    private lateinit var riskReasonText: TextView
    private lateinit var speakButton: Button
    private lateinit var callButton: Button
    private lateinit var nameInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var darkThemeSwitch: Switch
    private lateinit var speechRateLabel: TextView
    private lateinit var speechRateSeekBar: SeekBar
    private lateinit var deepSeekSwitch: Switch
    private lateinit var emergencyForwardSwitch: Switch
    private lateinit var aiBackendStatusText: TextView
    private lateinit var apiKeyInput: EditText
    private var currentMessage: IncomingMessage? = null
    private var renderedVersion = -1L
    private val uiHandler = Handler(Looper.getMainLooper())
    private val notificationConnectionCheck = Runnable {
        if (isFinishing || isDestroyed) return@Runnable
        if (!NotificationAccessState.isEnabled(this)) return@Runnable
        if (IncomingNotificationListenerService.isConnected()) {
            notificationAccessText.setText(R.string.notification_access_ready)
            notificationAccessButton.visibility = View.GONE
        } else {
            notificationAccessText.setText(R.string.notification_access_disconnected)
            notificationAccessButton.setText(R.string.notification_access_reconnect)
            notificationAccessButton.visibility = View.VISIBLE
        }
    }
    private val latestMessageListener: (IncomingMessage) -> Unit = { message ->
        runOnUiThread { if (!isFinishing && !isDestroyed) renderMessage(message) }
    }

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(ThemeContext.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MessageNotification.initialize(this)
        MessageGuardService.start(this)
        contactStore = EmergencyContactStore(this)
        preferences = UserPreferences(this)
        apiKeyStore = ApiKeyStore(this)
        bindViews()
        showSavedContact()
        initializeSettingsControls()
        bindActions()
        messageFromIntent(intent)?.let(::renderMessage)
        focusContactIfRequested(intent)
    }

    override fun onResume() {
        super.onResume()
        MessageGuardService.start(this)
        NotificationAccessState.requestRebindIfEnabled(this)
        refreshPermissionStatus()
        refreshNotificationAppsSummary()
        if (LatestMessageStore.version != renderedVersion) {
            LatestMessageStore.latest?.let(::renderMessage)
        }
    }

    override fun onStart() { super.onStart(); LatestMessageStore.addListener(latestMessageListener) }
    override fun onStop() { uiHandler.removeCallbacks(notificationConnectionCheck); LatestMessageStore.removeListener(latestMessageListener); super.onStop() }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        messageFromIntent(intent)?.let(::renderMessage)
        focusContactIfRequested(intent)
    }

    private fun bindViews() {
        mainScroll = findViewById(R.id.mainScroll); contactCard = findViewById(R.id.contactCard)
        statusText = findViewById(R.id.statusText); permissionButton = findViewById(R.id.permissionButton)
        notificationAccessText = findViewById(R.id.notificationAccessText)
        notificationAccessButton = findViewById(R.id.notificationAccessButton)
        notificationAppsButton = findViewById(R.id.notificationAppsButton)
        notificationAppsSummary = findViewById(R.id.notificationAppsSummary)
        senderText = findViewById(R.id.senderText); messageText = findViewById(R.id.messageText)
        riskPanel = findViewById(R.id.riskPanel); riskHeadingText = findViewById(R.id.riskHeadingText)
        riskReasonText = findViewById(R.id.riskReasonText); speakButton = findViewById(R.id.speakButton)
        callButton = findViewById(R.id.callButton); nameInput = findViewById(R.id.contactNameInput)
        phoneInput = findViewById(R.id.contactPhoneInput); darkThemeSwitch = findViewById(R.id.darkThemeSwitch)
        speechRateLabel = findViewById(R.id.speechRateLabel); speechRateSeekBar = findViewById(R.id.speechRateSeekBar)
        deepSeekSwitch = findViewById(R.id.deepSeekSwitch); emergencyForwardSwitch = findViewById(R.id.emergencyForwardSwitch)
        aiBackendStatusText = findViewById(R.id.aiBackendStatusText)
        apiKeyInput = findViewById(R.id.apiKeyInput)
    }

    private fun initializeSettingsControls() {
        darkThemeSwitch.isChecked = preferences.darkTheme
        speechRateSeekBar.progress = ((preferences.speechRate - SpeechRatePolicy.MIN) * 10).roundToInt().coerceIn(0, speechRateSeekBar.max)
        updateSpeechRateLabel(preferences.speechRate)
        val configured = apiKeyStore.hasKey()
        if (!configured) preferences.deepSeekEnabled = false
        deepSeekSwitch.isChecked = preferences.deepSeekEnabled
        aiBackendStatusText.setText(if (configured) R.string.ai_key_ready else R.string.ai_key_missing)
        val canForward = checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED && contactStore.read() != null
        if (!canForward) preferences.emergencyForwardingEnabled = false
        emergencyForwardSwitch.isChecked = preferences.emergencyForwardingEnabled
    }

    private fun bindActions() {
        permissionButton.setOnClickListener { requestRequiredPermissions() }
        notificationAccessButton.setOnClickListener { openNotificationAccessSettings() }
        notificationAppsButton.setOnClickListener { startActivity(Intent(this, NotificationAppsActivity::class.java)) }
        findViewById<Button>(R.id.testNotificationButton).setOnClickListener { showTestNotification() }
        findViewById<Button>(R.id.historyButton).setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        speakButton.setOnClickListener { currentMessage?.let { SpeechService.start(this, it) } }
        callButton.setOnClickListener { handleEmergencyCall() }
        findViewById<Button>(R.id.saveContactButton).setOnClickListener { saveContact() }
        findViewById<Button>(R.id.clearContactButton).setOnClickListener { clearContact() }
        findViewById<Button>(R.id.saveApiKeyButton).setOnClickListener { saveApiKey() }
        findViewById<Button>(R.id.clearApiKeyButton).setOnClickListener { clearApiKey() }
        darkThemeSwitch.setOnCheckedChangeListener { _, checked ->
            if (preferences.darkTheme != checked) { preferences.darkTheme = checked; recreate() }
        }
        speechRateSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val rate = SpeechRatePolicy.normalize(SpeechRatePolicy.MIN + progress * 0.10f)
                updateSpeechRateLabel(rate)
                if (fromUser) preferences.speechRate = rate
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        deepSeekSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked && !apiKeyStore.hasKey()) {
                deepSeekSwitch.isChecked = false
                Toast.makeText(this, R.string.ai_key_missing_toast, Toast.LENGTH_LONG).show()
                apiKeyInput.requestFocus()
            } else if (checked && !preferences.deepSeekEnabled) {
                showAiConsentDialog()
            } else {
                preferences.deepSeekEnabled = checked
            }
        }
        emergencyForwardSwitch.setOnCheckedChangeListener { _, checked ->
            if (!checked) preferences.emergencyForwardingEnabled = false
            else if (contactStore.read() == null) {
                emergencyForwardSwitch.isChecked = false
                Toast.makeText(this, "请先保存紧急联系人", Toast.LENGTH_LONG).show(); phoneInput.requestFocus()
            } else enableEmergencyForwarding()
        }
    }

    private fun saveApiKey() {
        if (!apiKeyStore.save(apiKeyInput.text.toString())) {
            apiKeyInput.error = getString(R.string.ai_key_invalid)
            apiKeyInput.requestFocus()
            return
        }
        apiKeyInput.setText("")
        aiBackendStatusText.setText(R.string.ai_key_ready)
        Toast.makeText(this, R.string.ai_key_saved, Toast.LENGTH_LONG).show()
    }

    private fun clearApiKey() {
        apiKeyStore.clear()
        preferences.deepSeekEnabled = false
        deepSeekSwitch.isChecked = false
        apiKeyInput.setText("")
        aiBackendStatusText.setText(R.string.ai_key_missing)
        Toast.makeText(this, R.string.ai_key_deleted, Toast.LENGTH_SHORT).show()
    }

    private fun showAiConsentDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.ai_consent_title)
            .setMessage(R.string.ai_consent_message)
            .setNegativeButton(R.string.cancel) { _, _ -> deepSeekSwitch.isChecked = false }
            .setPositiveButton(R.string.ai_consent_accept) { _, _ ->
                preferences.deepSeekEnabled = true
                Toast.makeText(this, R.string.ai_enabled_toast, Toast.LENGTH_SHORT).show()
            }
            .setOnCancelListener { deepSeekSwitch.isChecked = false }
            .show()
    }

    private fun refreshPermissionStatus() {
        val sms = checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val notification = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val ready = sms && notification
        statusText.setText(if (ready) R.string.permissions_ready else R.string.permissions_missing)
        permissionButton.visibility = if (ready) View.GONE else View.VISIBLE
        val access = NotificationAccessState.isEnabled(this)
        notificationAccessText.setText(if (access) R.string.notification_access_ready else R.string.notification_access_missing)
        notificationAccessButton.setText(R.string.enable_notification_access)
        notificationAccessButton.visibility = if (access) View.GONE else View.VISIBLE
        uiHandler.removeCallbacks(notificationConnectionCheck)
        if (access) {
            longArrayOf(1_500L, 4_000L, 8_000L).forEach { uiHandler.postDelayed(notificationConnectionCheck, it) }
        }
    }

    private fun refreshNotificationAppsSummary() {
        val selected = NotificationCapturePreferences(this).selectedPackages()
        if (selected.isEmpty()) {
            notificationAppsSummary.setText(R.string.notification_apps_summary_none)
            return
        }
        val labels = selected.map { packageName ->
            runCatching {
                val info = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(info).toString()
            }.getOrDefault(packageName)
        }
        val shown = labels.take(3).joinToString("、")
        notificationAppsSummary.text = if (labels.size > 3) {
            getString(R.string.notification_apps_summary_more, labels.size, shown)
        } else {
            getString(R.string.notification_apps_summary, labels.size, shown)
        }
    }

    private fun requestRequiredPermissions() {
        val missing = buildList {
            if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_SMS)
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (missing.isEmpty()) refreshPermissionStatus() else requestPermissions(missing.toTypedArray(), PERMISSION_REQUEST)
    }

    private fun enableEmergencyForwarding() {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            preferences.emergencyForwardingEnabled = true
            Toast.makeText(this, "危险信息同步已开启", Toast.LENGTH_SHORT).show()
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)) Toast.makeText(this, "仅当智能判断为真正危险时，才会给紧急联系人发送提醒短信。", Toast.LENGTH_LONG).show()
            requestPermissions(arrayOf(Manifest.permission.SEND_SMS), SEND_SMS_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            refreshPermissionStatus()
            if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "需要短信和通知权限才能及时提醒。", Toast.LENGTH_LONG).show()
                val permanentlyDenied = permissions.indices.any { index ->
                    grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED &&
                        !shouldShowRequestPermissionRationale(permissions[index])
                }
                if (permanentlyDenied) {
                    permissionButton.text = "前往系统设置开启"
                    permissionButton.setOnClickListener { openAppSettings() }
                }
            }
        } else if (requestCode == SEND_SMS_PERMISSION_REQUEST) {
            val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            preferences.emergencyForwardingEnabled = granted; emergencyForwardSwitch.isChecked = granted
            if (!granted) {
                Toast.makeText(this, "未获得发短信权限，危险信息不会同步。请在系统设置中开启后再试。", Toast.LENGTH_LONG).show()
                if (!shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)) openAppSettings()
            }
        }
    }

    private fun renderMessage(message: IncomingMessage) {
        currentMessage = message; renderedVersion = LatestMessageStore.version; senderText.text = getString(R.string.sender_format, message.sender); messageText.text = message.body
        speakButton.isEnabled = true
        val warning = message.riskLevel != RiskLevel.SAFE
        riskPanel.visibility = if (warning) View.VISIBLE else View.GONE; callButton.visibility = if (warning) View.VISIBLE else View.GONE
        if (warning) {
            riskHeadingText.setText(if (message.riskLevel == RiskLevel.DANGEROUS) R.string.risk_dangerous_heading else R.string.risk_suspicious_heading)
            riskReasonText.text = message.riskReason.ifBlank { getString(R.string.risk_body) }
        }
        updateCallButton()
    }

    private fun messageFromIntent(intent: Intent?): IncomingMessage? {
        val body = MessageSanitizer.clean(intent?.getStringExtra(MessageContract.EXTRA_BODY).orEmpty())
        if (body.isBlank()) return null
        val sender = MessageSanitizer.clean(intent?.getStringExtra(MessageContract.EXTRA_SENDER).orEmpty()).ifBlank { "未知号码" }.take(80)
        val localRisky = MessageRiskDetector.isRisky(body)
        val level = intent?.getStringExtra(MessageContract.EXTRA_RISK_LEVEL)?.let { runCatching { RiskLevel.valueOf(it) }.getOrNull() }
            ?: if (localRisky) RiskLevel.SUSPICIOUS else RiskLevel.SAFE
        val reason = MessageSanitizer.clean(intent?.getStringExtra(MessageContract.EXTRA_RISK_REASON).orEmpty()).take(160)
        return IncomingMessage(sender, body, level != RiskLevel.SAFE, level, reason)
    }

    private fun saveContact() {
        if (!contactStore.save(nameInput.text.toString(), phoneInput.text.toString())) {
            phoneInput.error = "请输入有效电话号码"; phoneInput.requestFocus(); return
        }
        showSavedContact(); updateCallButton(); Toast.makeText(this, "紧急联系人已保存在本机", Toast.LENGTH_SHORT).show()
    }

    private fun clearContact() {
        contactStore.clear(); nameInput.setText(""); phoneInput.setText("")
        preferences.emergencyForwardingEnabled = false; emergencyForwardSwitch.isChecked = false; updateCallButton()
    }

    private fun showSavedContact() { contactStore.read()?.let { nameInput.setText(it.name); phoneInput.setText(it.number) } }

    private fun updateCallButton() {
        if (currentMessage?.riskLevel == RiskLevel.SAFE) return
        val contact = contactStore.read(); callButton.text = if (contact == null) "先设置紧急联系人" else "☎ 联系${contact.name}"
    }

    private fun handleEmergencyCall() {
        val contact = contactStore.read()
        if (contact == null) {
            mainScroll.smoothScrollTo(0, contactCard.top); phoneInput.requestFocus()
            Toast.makeText(this, "请先填写并保存一位可信任的联系人", Toast.LENGTH_LONG).show(); return
        }
        try { startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", contact.number, null))) }
        catch (_: ActivityNotFoundException) { Toast.makeText(this, "无法打开拨号应用，请手动拨打 ${contact.number}", Toast.LENGTH_LONG).show() }
    }

    private fun updateSpeechRateLabel(rate: Float) {
        val label = when { rate <= 0.70f -> "很慢"; rate <= 0.90f -> "适中"; rate <= 1.05f -> "稍快"; else -> "快速" }
        speechRateLabel.text = getString(R.string.speech_rate_format, label)
    }

    private fun focusContactIfRequested(intent: Intent?) {
        if (intent?.getBooleanExtra(MessageContract.EXTRA_FOCUS_CONTACT, false) != true) return
        mainScroll.post { mainScroll.smoothScrollTo(0, contactCard.top); phoneInput.requestFocus() }
        intent.removeExtra(MessageContract.EXTRA_FOCUS_CONTACT)
    }

    private fun openNotificationAccessSettings() {
        NotificationAccessState.requestRebindIfEnabled(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(this@MainActivity, IncomingNotificationListenerService::class.java).flattenToString(),
                )
            }
            try { startActivity(detail); return } catch (_: ActivityNotFoundException) { }
        }
        try { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        catch (_: ActivityNotFoundException) { Toast.makeText(this, "请在系统设置中搜索“通知使用权”。", Toast.LENGTH_LONG).show() }
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }

    private fun showTestNotification() {
        val message = IncomingMessage.fromRaw("易懂通测试", "这是一条测试提醒，验证码 666666，请勿告诉陌生人。")
        LatestMessageStore.publishTransient(message); MessageNotification.show(this, message); renderMessage(message)
        Toast.makeText(this, "已发送本地测试提醒，不会上传或通知紧急联系人。", Toast.LENGTH_LONG).show()
    }

    private companion object { const val PERMISSION_REQUEST = 1001; const val SEND_SMS_PERMISSION_REQUEST = 1002 }
}
