package com.yidongtong.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCapturePolicyTest {
    @Test
    fun capturesOnlySelectedPackages() {
        val selected = setOf("com.android.mms")

        assertTrue(NotificationCapturePolicy.shouldCapture("com.android.mms", selected, "com.yidongtong.guardian"))
        assertFalse(NotificationCapturePolicy.shouldCapture("com.tencent.mm", selected, "com.yidongtong.guardian"))
    }

    @Test
    fun capturesSmsCompanionPackageWhenSmsAppSelected() {
        val selected = setOf("com.android.mms")

        assertTrue(
            NotificationCapturePolicy.shouldCapture(
                "com.android.mms.service",
                selected,
                "com.yidongtong.guardian",
                "com.android.mms",
            ),
        )
    }

    @Test
    fun doesNotCaptureSmsCompanionPackageWhenSmsAppUnselected() {
        assertFalse(
            NotificationCapturePolicy.shouldCapture(
                "com.android.mms.service",
                setOf("com.tencent.mm"),
                "com.yidongtong.guardian",
                "com.android.mms",
            ),
        )
    }

    @Test
    fun doesNotCaptureSimilarPrefixWithoutDot() {
        assertFalse(
            NotificationCapturePolicy.shouldCapture(
                "com.android.mmsx",
                setOf("com.android.mms"),
                "com.yidongtong.guardian",
                "com.android.mms",
            ),
        )
    }

    @Test
    fun neverCapturesOwnPackage() {
        assertFalse(
            NotificationCapturePolicy.shouldCapture(
                "com.yidongtong.guardian",
                setOf("com.yidongtong.guardian"),
                "com.yidongtong.guardian",
            ),
        )
    }

    @Test
    fun defaultSelectionContainsOnlySmsApp() {
        assertEquals(setOf("com.android.mms"), NotificationCapturePolicy.defaultPackages("com.android.mms", "com.yidongtong.guardian"))
        assertEquals(emptySet<String>(), NotificationCapturePolicy.defaultPackages(null, "com.yidongtong.guardian"))
        assertEquals(emptySet<String>(), NotificationCapturePolicy.defaultPackages("com.yidongtong.guardian", "com.yidongtong.guardian"))
    }

    @Test
    fun normalizationTrimsAndRemovesOwnPackage() {
        assertEquals(
            setOf("com.android.mms", "com.tencent.mm"),
            NotificationCapturePolicy.normalize(
                setOf(" com.android.mms ", "", "com.yidongtong.guardian", "com.tencent.mm"),
                "com.yidongtong.guardian",
            ),
        )
    }
}