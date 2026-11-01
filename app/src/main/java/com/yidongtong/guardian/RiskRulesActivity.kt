package com.yidongtong.guardian

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.*

class RiskRulesActivity : Activity() {
    private lateinit var settings: RiskRulePreferences
    private lateinit var list: LinearLayout
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(ThemeContext.wrap(newBase))
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = RiskRulePreferences(this)
        showPage()
    }
    private fun showPage() {
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }
        scroll.setBackgroundColor(getColor(R.color.surface))
        scroll.addView(list)
        setContentView(scroll)
        button("返回") { finish() }
        label("安全规则库", 30f)
        label("命中严格规则即提醒风险，并非确认诈骗。验证码不限长度；取件码、“链接”字样、网址及转账均为危险。关闭某条本地规则后，DeepSeek 仍可能独立判断风险。设置只影响后续信息。")
        button("恢复默认：全部开启") {
            android.app.AlertDialog.Builder(this).setMessage("恢复所有规则为开启状态？")
                .setNegativeButton("取消", null).setPositiveButton("恢复") { _, _ -> settings.restoreDefaults(); showPage() }.show()
        }
        label("输入短信试判", 25f)
        val input = EditText(this).apply {
            hint = "输入或粘贴短信，仅在本机试判"
            setTextColor(getColor(R.color.ink)); setHintTextColor(getColor(R.color.ink_secondary))
            textSize = 21f; minLines = 3
            filters = arrayOf(android.text.InputFilter.LengthFilter(3000))
            isSaveEnabled = false
            importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        list.addView(input)
        val result = TextView(this).apply { textSize = 21f; setTextColor(getColor(R.color.ink)) }
        button("本地试判（不上传、不发短信）") {
            if (input.text.isBlank()) result.text = "请先输入短信内容"
            else {
                val a = RiskRuleLibrary.assess(input.text.toString(), settings.disabled())
                result.text = "${levelName(a.level)}\n${a.reason}"
            }
        }
        list.addView(result)
        for (level in listOf(RiskLevel.DANGEROUS, RiskLevel.SUSPICIOUS)) {
            label(if (level == RiskLevel.DANGEROUS) "危险规则" else "可疑规则", 27f)
            RiskRuleLibrary.rules.filter { it.level == level }.forEach { rule ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundResource(R.drawable.bg_card)
                }
                val toggle = Switch(this).apply {
                    text = "${rule.id}  ${rule.title}"
                    textSize = 22f; minHeight = (64 * resources.displayMetrics.density).toInt()
                    setTextColor(getColor(R.color.ink)); isChecked = rule.id !in settings.disabled()
                    setOnCheckedChangeListener { _, checked -> settings.setEnabled(rule.id, checked) }
                }
                FeatureIcons.attach(toggle, "shield", getColor(R.color.deep_green))
                card.addView(toggle)
                card.addView(TextView(this).apply {
                    text = "${levelName(rule.level)} · ${rule.description}\n示例：${rule.example}"
                    textSize = 19f; setTextColor(getColor(R.color.ink_secondary))
                })
                list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 16 })
            }
        }
        label("24类生活场景示例", 27f)
        label("示例供理解规则，分类随您当前启用的规则变化。")
        RiskRuleLibrary.scenarios.forEach { (name, body) ->
            val a = RiskRuleLibrary.assess(body, settings.disabled())
            label("$name · ${levelName(a.level)}\n$body\n${a.reason}", 19f)
        }
    }
    private fun label(value: String, size: Float = 20f) {
        list.addView(TextView(this).apply {
            text = value; textSize = size; setTextColor(getColor(R.color.ink)); setPadding(0, 20, 0, 12)
        })
    }
    private fun button(title: String, action: () -> Unit) {
        list.addView(Button(this).apply {
            text = title; textSize = 21f; isAllCaps = false
            minHeight = (60 * resources.displayMetrics.density).toInt()
            setOnClickListener { action() }
            FeatureIcons.attach(this, color = currentTextColor)
        })
    }
    private fun levelName(level: RiskLevel) = when (level) {
        RiskLevel.SAFE -> "暂未发现明显风险"
        RiskLevel.SUSPICIOUS -> "可疑"
        RiskLevel.DANGEROUS -> "危险"
    }
}
