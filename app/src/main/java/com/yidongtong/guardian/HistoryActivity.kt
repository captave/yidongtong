package com.yidongtong.guardian

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class HistoryActivity : Activity() {
    private lateinit var searchInput: EditText
    private lateinit var resultCount: TextView
    private lateinit var emptyText: TextView
    private lateinit var resultList: LinearLayout
    private val executor = Executors.newSingleThreadExecutor()

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(ThemeContext.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        searchInput = findViewById(R.id.historySearchInput)
        resultCount = findViewById(R.id.historyResultCount)
        emptyText = findViewById(R.id.historyEmptyText)
        resultList = findViewById(R.id.historyResultList)
        findViewById<Button>(R.id.historyBackButton).setOnClickListener { finish() }
        findViewById<Button>(R.id.historySearchButton).setOnClickListener { loadRecords(searchInput.text.toString()) }
        findViewById<Button>(R.id.historyClearSearchButton).setOnClickListener { searchInput.setText(""); loadRecords("") }
        findViewById<Button>(R.id.historyClearAllButton).setOnClickListener { confirmClearAll() }
        loadRecords("")
    }

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }

    private fun loadRecords(query: String) {
        executor.execute {
            val records = MessageHistoryDatabase.get(applicationContext).search(query)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                resultCount.text = getString(R.string.history_result_count, records.size)
                emptyText.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                resultList.removeAllViews()
                records.forEach { resultList.addView(createRecordView(it)) }
            }
        }
    }

    private fun createRecordView(record: MessageRecord): View {
        val view = layoutInflater.inflate(R.layout.item_history_record, resultList, false)
        val level = when (record.assessment.level) {
            RiskLevel.SAFE -> "安全"
            RiskLevel.SUSPICIOUS -> "需要核实"
            RiskLevel.DANGEROUS -> "高危"
        }
        view.findViewById<TextView>(R.id.recordLevel).text = level
        view.findViewById<TextView>(R.id.recordTime).text = DATE_FORMAT.format(Date(record.receivedAt))
        view.findViewById<TextView>(R.id.recordSender).text = getString(R.string.sender_format, record.sender)
        view.findViewById<TextView>(R.id.recordBody).text = record.body
        view.findViewById<TextView>(R.id.recordReason).text = getString(R.string.judgement_format, record.assessment.reason)
        view.findViewById<Button>(R.id.recordDeleteButton).setOnClickListener { confirmDelete(record) }
        return view
    }

    private fun confirmDelete(record: MessageRecord) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_record_title)
            .setMessage("将删除来自“${record.sender}”的这条记录。")
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                executor.execute {
                    val deleted = MessageHistoryDatabase.get(applicationContext).delete(record.id)
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        if (deleted) Toast.makeText(this, R.string.record_deleted, Toast.LENGTH_SHORT).show()
                        loadRecords(searchInput.text.toString())
                    }
                }
            }.show()
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_all_title)
            .setMessage(R.string.clear_all_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear_history) { _, _ ->
                executor.execute {
                    MessageHistoryDatabase.get(applicationContext).clearAll()
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show()
                        loadRecords("")
                    }
                }
            }.show()
    }

    private companion object {
        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    }
}
