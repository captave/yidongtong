package com.yidongtong.guardian

object NotificationCapturePolicy {
    fun defaultPackages(defaultSmsPackage: String?, ownPackage: String?): Set<String> {
        val normalized = defaultSmsPackage?.trim().orEmpty()
        if (normalized.isBlank() || normalized == ownPackage) return emptySet()
        return setOf(normalized)
    }

    fun shouldCapture(packageName: String, selectedPackages: Set<String>, ownPackage: String?): Boolean {
        if (packageName.isBlank() || packageName == ownPackage) return false
        return packageName in selectedPackages
    }

    fun normalize(packages: Set<String>, ownPackage: String?): Set<String> =
        packages.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && it != ownPackage }
            .toSet()
}
