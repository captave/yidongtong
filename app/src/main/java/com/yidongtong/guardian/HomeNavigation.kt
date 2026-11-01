package com.yidongtong.guardian

import android.app.Activity
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Keeps existing feature views and their listeners; only changes their presentation and navigation. */
class HomeNavigation(private val activity: Activity, private val scroll: ScrollView) {
    private val cards = linkedMapOf<String, View>()
    private val menus = mutableListOf<View>()
    private val tabs = mutableListOf<TextView>()
    private lateinit var content: LinearLayout
    private lateinit var heading: TextView
    private lateinit var back: TextView
    var tab = 0
        private set
    var detail: String? = null
        private set

    fun install(initialTab: Int, initialDetail: String?) {
        fun card(key: String, id: Int) {
            val view = activity.findViewById<View>(id).parent as View
            (view.parent as ViewGroup).removeView(view)
            cards[key] = view
        }
        card("permissions", R.id.statusText)
        card("message", R.id.senderText)
        card("analysis", R.id.aiBackendStatusText)
        val contact = activity.findViewById<View>(R.id.contactCard)
        (contact.parent as ViewGroup).removeView(contact); cards["contact"] = contact
        card("display", R.id.darkThemeSwitch)
        card("history", R.id.historyButton)
        (scroll.parent as? ViewGroup)?.removeView(scroll)
        scroll.removeAllViews()
        content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(24))
        }
        scroll.addView(content)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(color(R.color.surface))
        }
        val toolbar = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(color(R.color.card)); setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        back = text("‹ 返回", 20f).apply { minHeight = dp(52); gravity = Gravity.CENTER; setOnClickListener { select(tab) } }
        toolbar.addView(back)
        heading = text("易懂通", 25f).apply { gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD) }
        toolbar.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        toolbar.addView(Space(activity), LinearLayout.LayoutParams(dp(64), 1))
        root.addView(toolbar)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(View(activity).apply { setBackgroundColor(color(R.color.line)) }, LinearLayout.LayoutParams(-1, dp(1)))
        val nav = LinearLayout(activity).apply { setBackgroundColor(color(R.color.card)); gravity = Gravity.CENTER }
        listOf("消息", "守护", "记录", "我的").forEachIndexed { index, label ->
            val item = text(label, 20f).apply {
                gravity = Gravity.CENTER; setPadding(dp(4), dp(8), dp(4), dp(8))
                minHeight = dp(76); contentDescription = listOf("消息", "守护", "记录", "我的")[index]
                setOnClickListener { select(index) }
            }
            tabs += item; nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)
        activity.setContentView(root)
        // Respect status/navigation bars and keyboards, including gesture navigation.
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.ime())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        root.requestApplyInsets()
        cards.values.forEach { content.addView(it); it.visibility = View.GONE }
        select(initialTab.coerceIn(0, 3), initialDetail?.takeIf { it in cards })
    }

    fun select(index: Int, page: String? = null) {
        tab = index; detail = page
        menus.forEach { content.removeView(it) }; menus.clear()
        cards.values.forEach { it.visibility = View.GONE }
        back.visibility = if (page == null) View.GONE else View.VISIBLE
        val titles = mapOf("permissions" to "接收与通知", "analysis" to "智能分析与家人通知", "contact" to "紧急联系人", "display" to "显示与朗读")
        heading.text = if (page != null) titles[page] ?: "易懂通" else listOf("易懂通", "安全守护", "信息记录", "我的")[index]
        tabs.forEachIndexed { i, v ->
            v.setTextColor(color(if (i == index) R.color.deep_green else R.color.ink_secondary))
            v.isSelected = i == index
            v.setTypeface(null, if (i == index) Typeface.BOLD else Typeface.NORMAL)
            FeatureIcons.attach(v, listOf("message", "shield", "history", "person")[i],
                color(if (i == index) R.color.deep_green else R.color.ink_secondary), top = true, size = 27)
        }
        if (page != null) cards[page]?.visibility = View.VISIBLE else when (index) {
            0 -> {
                intro("消息", "看得清，听得懂。点击朗读，轻松了解最新信息。")
                cards["message"]?.visibility = View.VISIBLE
                row("查看历史消息", "查询与管理近90天记录", { activity.startActivity(android.content.Intent(activity, HistoryActivity::class.java)) })
                row("接收状态", "短信权限与通知读取范围", { select(1, "permissions") })
            }
            1 -> {
                intro("安心守护", "选择防护规则，让重要提醒及时到达。")
                row("安全规则库", "危险规则 · 可疑规则 · 短信试判", { activity.startActivity(android.content.Intent(activity, RiskRulesActivity::class.java)) })
                row("接收与通知", "权限设置与允许读取的应用", { select(1, "permissions") })
                row("智能分析与家人通知", "DeepSeek 配置与同步开关", { select(1, "analysis") })
                row("紧急联系人", "设置一位可信任的家人", { select(1, "contact") })
            }
            2 -> {
                intro("信息记录", "记录保存在本机，90天后自动清理。")
                cards["history"]?.visibility = View.VISIBLE
            }
            3 -> {
                intro("易懂通", "您的随身信息助手")
                row("紧急联系人", "编辑称呼和联系电话", { select(3, "contact") })
                row("显示与朗读", "浅色 / 深色主题 · 普通话语速", { select(3, "display") })
                row("关于易懂通", "当前版本 ${activity.packageManager.getPackageInfo(activity.packageName, 0).versionName} · 记录仅保留90天", {
                    android.app.AlertDialog.Builder(activity).setTitle("易懂通").setMessage("面向老年人的信息阅读与安全守护工具。\n\n云端分析与家人同步由您自行开启；命中规则并不表示已确认诈骗。").setPositiveButton("知道了", null).show()
                })
            }
        }
        scroll.scrollTo(0, 0)
    }
    private fun intro(title: String, subtitle: String) {
        val view = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(4), dp(8), dp(4), dp(20))
            addView(text(title, 30f).apply { setTypeface(null, Typeface.BOLD) })
            addView(text(subtitle, 19f).apply { setTextColor(color(R.color.ink_secondary)); setPadding(0, dp(8), 0, 0) })
        }
        menus += view; content.addView(view, 0)
    }
    private fun row(title: String, subtitle: String, action: () -> Unit) {
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundResource(R.drawable.bg_card)
            minimumHeight = dp(90); isClickable = true; isFocusable = true
            contentDescription = "$title，$subtitle"
            addView(TextView(activity).apply {
                setBackgroundResource(R.drawable.bg_status)
                setPadding(dp(10), dp(10), dp(10), dp(10))
                FeatureIcons.attach(this, FeatureIcons.kind(title), color(R.color.deep_green), size = 29)
            }, LinearLayout.LayoutParams(dp(54), dp(54)))
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(16), 0, dp(8), 0)
                addView(text(title, 23f).apply { setTypeface(null, Typeface.BOLD) })
                addView(text(subtitle, 18f).apply { setTextColor(color(R.color.ink_secondary)); setPadding(0, dp(6), 0, 0) })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(text("›", 28f).apply { setTextColor(color(R.color.ink_secondary)) })
            setOnClickListener { action() }
        }
        menus += box; content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
    }
    private fun text(value: String, size: Float) = TextView(activity).apply { text = value; textSize = size; setTextColor(color(R.color.ink)) }
    private fun color(id: Int) = activity.getColor(id)
    private fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
}
