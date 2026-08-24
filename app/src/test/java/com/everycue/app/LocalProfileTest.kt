package com.everycue.app

import org.junit.Assert.assertTrue
import org.junit.Test

class LocalProfileTest {
    @Test
    fun shareTextContainsAllUserControlledFields() {
        val profile = LocalProfile("Asha", "Rao", "+91", "9876543210", "asha@example.com", "Line 1\nLine 2", "560001")
        profile.validate()
        val shared = profile.asShareText(
            phone = "Phone: +91 9876543210",
            emailLabel = "Email: asha@example.com",
            addressLabel = "Address:",
            pincodeLabel = "Pincode: 560001",
        )
        listOf("Asha Rao", "+91 9876543210", "asha@example.com", "Line 1", "Line 2", "560001")
            .forEach { assertTrue(shared.contains(it)) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun addressRejectsMoreThanFiveLines() {
        LocalProfile("Asha", "Rao", "+91", "9876543210", "asha@example.com", "1\n2\n3\n4\n5\n6", "560001").validate()
    }
}

