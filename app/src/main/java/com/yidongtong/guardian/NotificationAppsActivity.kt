package com.yidongtong.guardian

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class NotificationAppsActivity : Activity() {
    private lateinit var capturePreferences: NotificationCapturePreferences
    private lateinit var listView: ListView
    private lateinit var emptyText: TextView
    private lateinit var selectionSummary: TextView
    private lateinit var adapter: NotificationAppAdapter
    private var apps: List<NotificationAppInfo> = emptyList()
    private var selectedPackages: Set<String> = emptySet()
    private val executor = Executors.newSingleThreadExecutor()

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(ThemeContext.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_apps)
        capturePreferences = NotificationCapturePreferences(this)
        selectedPackages = capturePreferences.selectedPackages()
        listView = findViewById(R.id.notificationAppsList)
        emptyText = findViewById(R.id.notificationAppsEmpty)
        selectionSummary = findViewById(R.id.notificationAppsSelectionSummary)
        adapter = NotificationAppAdapter()
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ -> toggle(apps[position]) }
        findViewById<ImageButton>(R.id.notificationAppsBackButton).setOnClickListener { finish() }
        findViewById<Button>(R.id.notificationAppsKeepSmsButton).setOnClickListener { keepOnlySms() }
        updateSelectionSummary()
        loadApps()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun loadApps() {
        executor.execute {
            val loaded = runCatching { NotificationAppCatalog.load(applicationContext) }.getOrDefault(emptyList())
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                apps = loaded
                adapter.notifyDataSetChanged()
                emptyText.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
                listView.visibility = if (apps.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun toggle(app: NotificationAppInfo) {
        val updated = selectedPackages.toMutableSet()
        if (!updated.add(app.packageName)) updated.remove(app.packageName)
        selectedPackages = updated
        capturePreferences.setSelectedPackages(selectedPackages)
        adapter.notifyDataSetChanged()
        updateSelectionSummary()
    }

    private fun keepOnlySms() {
        selectedPackages = capturePreferences.defaultPackages()
        capturePreferences.setSelectedPackages(selectedPackages)
        adapter.notifyDataSetChanged()
        updateSelectionSummary()
        val message = if (selectedPackages.isEmpty()) {
            R.string.notification_apps_no_sms
        } else {
            R.string.notification_apps_keep_sms_done
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateSelectionSummary() {
        selectionSummary.text = getString(R.string.notification_apps_selected_count, selectedPackages.size)
    }

    private inner class NotificationAppAdapter : BaseAdapter() {
        override fun getCount(): Int = apps.size
        override fun getItem(position: Int): NotificationAppInfo = apps[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_notification_app, parent, false)
            val app = apps[position]
            view.findViewById<ImageView>(R.id.notificationAppIcon).setImageDrawable(app.icon)
            view.findViewById<TextView>(R.id.notificationAppName).text = app.label
            view.findViewById<TextView>(R.id.notificationAppDetail).text =
                if (app.isDefaultSms) getString(R.string.notification_apps_default_sms) else app.packageName
            view.findViewById<Switch>(R.id.notificationAppSwitch).isChecked =
                app.packageName in selectedPackages
            return view
        }
    }
}
