package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationContentSelectorTest {
    @Test
    fun expandedTextIsPreferredAndTitleBecomesSender() {
        val content = NotificationContentSelector.select(
            appName = "信息",
            title = "10086",
            text = "您的验证码是",
            bigText = "您的验证码是 666666，请勿泄露",
            textLines = emptyList(),
        )

        assertEquals("10086", content?.sender)
        assertEquals("您的验证码是 666666，请勿泄露", content?.body)
    }

    @Test
    fun textLinesAreUsedWhenOtherTextIsMissing() {
        val content = NotificationContentSelector.select(
            appName = "信息",
            title = "",
            text = "",
            bigText = "",
            textLines = listOf("第一行", "第二行"),
        )

        assertEquals("信息", content?.sender)
        assertEquals("第一行\n第二行", content?.body)
    }

    @Test
    fun notificationWithoutReadableTextIsIgnored() {
        assertNull(
            NotificationContentSelector.select(
                appName = "应用",
                title = "标题",
                text = "",
                bigText = "",
                textLines = emptyList(),
            ),
        )
    }
}
