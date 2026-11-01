package com.yidongtong.guardian

object NotificationCapturePolicy {
    fun defaultPackages(defaultSmsPackage: String?, ownPackage: String?): Set<String> {
        val normalized = defaultSmsPackage?.trim().orEmpty()
        if (normalized.isBlank() || normalized == ownPackage) return emptySet()
        return setOf(normalized)
    }

    fun shouldCapture(
        packageName: String,
        selectedPackages: Set<String>,
        ownPackage: String?,
        defaultSmsPackage: String? = null,
    ): Boolean {
        if (packageName.isBlank() || packageName == ownPackage) return false
        if (packageName in selectedPackages) return true
        // 部分厂商（如 vivo）由短信应用的伴生包发出通知，例如
        // com.android.mms.service；选中系统短信应用时一并放行。
        val smsPackage = defaultSmsPackage?.trim().orEmpty()
        if (smsPackage.isBlank() || smsPackage !in selectedPackages) return false
        return packageName.startsWith("$smsPackage.")
    }

    fun normalize(packages: Set<String>, ownPackage: String?): Set<String> =
        packages.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && it != ownPackage }
            .toSet()
}