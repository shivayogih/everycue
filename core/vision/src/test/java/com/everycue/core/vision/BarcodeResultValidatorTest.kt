package com.everycue.core.vision

import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeResultValidatorTest {
    @Test
    fun `accepts decoded product codes with valid check digits`() {
        assertEquals("4006381333931", BarcodeResultValidator.normalize(" 4006381333931 ", Barcode.FORMAT_EAN_13))
        assertEquals("96385074", BarcodeResultValidator.normalize("96385074", Barcode.FORMAT_EAN_8))
        assertEquals("036000291452", BarcodeResultValidator.normalize("036000291452", Barcode.FORMAT_UPC_A))
    }

    @Test
    fun `rejects corrupted gtin values instead of silently storing them`() {
        assertNull(BarcodeResultValidator.normalize("4006381333932", Barcode.FORMAT_EAN_13))
        assertNull(BarcodeResultValidator.normalize("not-a-gtin", Barcode.FORMAT_EAN_13))
    }

    @Test
    fun `keeps valid non gtin payloads but rejects unsafe control characters`() {
        assertEquals("LOT-A17", BarcodeResultValidator.normalize("LOT-A17", Barcode.FORMAT_CODE_128))
        assertNull(BarcodeResultValidator.normalize("LOT\nA17", Barcode.FORMAT_QR_CODE))
    }
}

