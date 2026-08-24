package com.everycue.app

import android.os.Build
import java.io.File

enum class SecurityBlockReason { ROOTED, EMULATOR }

sealed interface DeviceSecurityVerdict {
    data object Trusted : DeviceSecurityVerdict
    data class Blocked(val reason: SecurityBlockReason) : DeviceSecurityVerdict
}

data class DeviceSecuritySignals(
    val fingerprint: String = "",
    val model: String = "",
    val manufacturer: String = "",
    val brand: String = "",
    val device: String = "",
    val product: String = "",
    val hardware: String = "",
    val buildTags: String = "",
    val rootArtifactPresent: Boolean = false,
)

object DeviceSecurityEvaluator {
    fun evaluate(signals: DeviceSecuritySignals, allowInsecureDevice: Boolean): DeviceSecurityVerdict {
        if (allowInsecureDevice) return DeviceSecurityVerdict.Trusted
        if (signals.looksLikeEmulator()) return DeviceSecurityVerdict.Blocked(SecurityBlockReason.EMULATOR)
        if (signals.rootArtifactPresent || signals.buildTags.contains("test-keys", ignoreCase = true)) {
            return DeviceSecurityVerdict.Blocked(SecurityBlockReason.ROOTED)
        }
        return DeviceSecurityVerdict.Trusted
    }

    private fun DeviceSecuritySignals.looksLikeEmulator(): Boolean {
        val values = listOf(fingerprint, model, manufacturer, brand, device, product, hardware).map(String::lowercase)
        return fingerprint.startsWith("generic", true) ||
            fingerprint.startsWith("unknown", true) ||
            model.contains("google_sdk", true) ||
            model.contains("emulator", true) ||
            model.contains("android sdk built for", true) ||
            model.contains("sdk_gphone", true) ||
            manufacturer.contains("genymotion", true) ||
            (brand.startsWith("generic", true) && device.startsWith("generic", true)) ||
            product.contains("sdk", true) ||
            product.contains("vbox", true) ||
            hardware.contains("goldfish", true) ||
            hardware.contains("ranchu", true) ||
            hardware.contains("qemu", true) ||
            values.any { it.contains("bluestacks") || it.contains("nox") }
    }
}

object DeviceSecurityGuard {
    private val rootArtifacts = listOf(
        "/system/app/Superuser.apk",
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/data/local/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
        "/system/bin/.ext/.su",
        "/system/xbin/daemonsu",
        "/sbin/.magisk",
        "/data/adb/magisk",
    )

    fun verdict(allowInsecureDevice: Boolean = BuildConfig.ALLOW_INSECURE_DEVICE): DeviceSecurityVerdict =
        DeviceSecurityEvaluator.evaluate(
            signals = DeviceSecuritySignals(
                fingerprint = Build.FINGERPRINT.orEmpty(),
                model = Build.MODEL.orEmpty(),
                manufacturer = Build.MANUFACTURER.orEmpty(),
                brand = Build.BRAND.orEmpty(),
                device = Build.DEVICE.orEmpty(),
                product = Build.PRODUCT.orEmpty(),
                hardware = Build.HARDWARE.orEmpty(),
                buildTags = Build.TAGS.orEmpty(),
                rootArtifactPresent = rootArtifacts.any { File(it).exists() },
            ),
            allowInsecureDevice = allowInsecureDevice,
        )

    fun isAccessAllowed(): Boolean = verdict() == DeviceSecurityVerdict.Trusted
}
