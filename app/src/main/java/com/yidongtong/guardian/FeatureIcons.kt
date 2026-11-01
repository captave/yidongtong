package com.yidongtong.guardian

import android.graphics.*
import android.graphics.drawable.Drawable
import android.widget.TextView

/** Theme-aware line icons. Paths share a 24-unit grid and rounded strokes. */
object FeatureIcons {
    fun decorate(root: android.view.View) {
        if (root is android.view.ViewGroup) {
            for (i in 0 until root.childCount) decorate(root.getChildAt(i))
        }
        if (root is android.widget.Button || root is android.widget.Switch) {
            val label = root as TextView
            // Existing labels retain their accessible names; icons are decorative.
            attach(label, color = label.currentTextColor, size = 23)
        }
    }
    fun kind(text: String): String = when {
        text.contains("返回") -> "back"
        text.contains("删除") || text.contains("清除") || text.contains("清空") -> "delete"
        text.contains("保存") -> "save"
        text.contains("查询") || text.contains("搜索") || text.contains("试判") -> "search"
        text.contains("规则") || text.contains("危险") || text.contains("守护") -> "shield"
        text.contains("Key") || text.contains("密钥") -> "key"
        text.contains("DeepSeek") || text.contains("智能") -> "ai"
        text.contains("家人") || text.contains("联系人") || text.contains("联系") -> "contact"
        text.contains("主题") || text.contains("显示") -> "theme"
        text.contains("朗读") || text.contains("语速") -> "voice"
        text.contains("通知") || text.contains("接收") || text.contains("提醒") -> "bell"
        text.contains("记录") || text.contains("历史") -> "history"
        text.contains("关于") -> "info"
        text.contains("恢复") -> "reset"
        text.contains("全部") -> "list"
        else -> "message"
    }

    fun attach(view: TextView, name: String = kind(view.text.toString()), color: Int, top: Boolean = false, size: Int = 25) {
        val icon = Icon(name, color)
        val px = (size * view.resources.displayMetrics.density).toInt()
        icon.setBounds(0, 0, px, px)
        view.compoundDrawablePadding = (10 * view.resources.displayMetrics.density).toInt()
        if (top) view.setCompoundDrawables(null, icon, null, null)
        else view.setCompoundDrawablesRelative(icon, null, null, null)
    }

    private class Icon(private val name: String, color: Int) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.8f
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        override fun draw(canvas: Canvas) {
            val save = canvas.save()
            canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
            canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
            fun path(vararg points: Float) {
                val p = Path(); p.moveTo(points[0], points[1])
                for (i in 2 until points.size step 2) p.lineTo(points[i], points[i + 1])
                canvas.drawPath(p, paint)
            }
            fun circle(x: Float, y: Float, r: Float) = canvas.drawCircle(x, y, r, paint)
            when (name) {
                "shield" -> { path(12f,2f,21f,6f,20f,14f,17f,19f,12f,22f,7f,19f,4f,14f,3f,6f,12f,2f); path(8f,12f,11f,15f,16f,9f) }
                "contact", "person" -> { circle(12f,7f,4f); canvas.drawArc(RectF(4f,13f,20f,27f),180f,180f,false,paint) }
                "history" -> { circle(12f,12f,9f); path(12f,6f,12f,12f,16f,14f) }
                "voice" -> { path(3f,9f,7f,9f,12f,5f,12f,19f,7f,15f,3f,15f,3f,9f); canvas.drawArc(RectF(10f,5f,22f,19f),-60f,120f,false,paint); canvas.drawArc(RectF(12f,8f,18f,16f),-60f,120f,false,paint) }
                "bell" -> { path(5f,17f,7f,14f,7f,9f,8f,6f,12f,4f,16f,6f,17f,9f,17f,14f,19f,17f,5f,17f); path(10f,21f,14f,21f); path(12f,2f,12f,4f) }
                "theme" -> { circle(12f,12f,9f); val filled = Paint(paint).apply { style = Paint.Style.FILL }; canvas.drawArc(RectF(3f,3f,21f,21f),90f,180f,false,filled) }
                "search" -> { circle(10f,10f,6.5f); path(15f,15f,21f,21f) }
                "key" -> { circle(8f,9f,5f); path(12f,12f,21f,21f,23f,19f,20f,16f,18f,18f) }
                "ai" -> { canvas.drawRoundRect(RectF(5f,5f,19f,19f),3f,3f,paint); for (v in listOf(8f,16f)) { path(v,2f,v,5f); path(v,19f,v,22f); path(2f,v,5f,v); path(19f,v,22f,v) }; path(9f,15f,12f,9f,15f,15f); path(10f,13f,14f,13f) }
                "delete" -> { path(4f,6f,20f,6f); path(9f,3f,15f,3f); path(6f,6f,7f,21f,17f,21f,18f,6f); path(10f,10f,10f,17f); path(14f,10f,14f,17f) }
                "save" -> { path(4f,3f,17f,3f,21f,7f,21f,21f,3f,21f,3f,3f); path(8f,3f,8f,9f,16f,9f,16f,3f); path(7f,21f,7f,14f,17f,14f,17f,21f) }
                "back" -> path(15f,4f,7f,12f,15f,20f)
                "info" -> { circle(12f,12f,9f); path(12f,11f,12f,17f); circle(12f,7f,0.6f) }
                "reset" -> { canvas.drawArc(RectF(4f,4f,20f,20f),-150f,300f,false,paint); path(3f,3f,3f,9f,9f,9f) }
                "list" -> { for (v in listOf(6f,12f,18f)) { circle(4f,v,0.6f); path(9f,v,21f,v) } }
                else -> { canvas.drawRoundRect(RectF(3f,4f,21f,18f),4f,4f,paint); path(7f,18f,6f,22f,12f,18f); path(7f,9f,17f,9f); path(7f,13f,14f,13f) }
            }
            canvas.restoreToCount(save)
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
        override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter; invalidateSelf() }
        @Deprecated("Deprecated in Android") override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}
