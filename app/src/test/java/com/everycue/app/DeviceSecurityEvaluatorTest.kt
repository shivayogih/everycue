package com.everycue.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceSecurityEvaluatorTest {
    @Test
    fun releaseBlocksEmulatorSignals() {
        val result = DeviceSecurityEvaluator.evaluate(
            DeviceSecuritySignals(model = "Android SDK built for x86", hardware = "ranchu"),
            allowInsecureDevice = false,
        )
        assertEquals(DeviceSecurityVerdict.Blocked(SecurityBlockReason.EMULATOR), result)
    }

    @Test
    fun releaseBlocksRootSignals() {
        val result = DeviceSecurityEvaluator.evaluate(
            DeviceSecuritySignals(buildTags = "release-keys test-keys", rootArtifactPresent = true),
            allowInsecureDevice = false,
        )
        assertEquals(DeviceSecurityVerdict.Blocked(SecurityBlockReason.ROOTED), result)
    }

    @Test
    fun debugAllowsDevelopmentEnvironments() {
        val result = DeviceSecurityEvaluator.evaluate(
            DeviceSecuritySignals(model = "Emulator", rootArtifactPresent = true),
            allowInsecureDevice = true,
        )
        assertEquals(DeviceSecurityVerdict.Trusted, result)
    }

    @Test
    fun releaseAllowsOrdinaryProductionDevice() {
        val result = DeviceSecurityEvaluator.evaluate(
            DeviceSecuritySignals(
                fingerprint = "vendor/device/device:16/release:user/release-keys",
                model = "Production Phone",
                manufacturer = "Vendor",
                brand = "Vendor",
                device = "device",
                product = "device_global",
                hardware = "soc",
                buildTags = "release-keys",
            ),
            allowInsecureDevice = false,
        )
        assertEquals(DeviceSecurityVerdict.Trusted, result)
    }
}
