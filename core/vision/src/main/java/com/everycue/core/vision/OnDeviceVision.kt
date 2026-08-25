package com.everycue.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

enum class VisionFailure {
    MODEL_UNAVAILABLE,
    IMAGE_UNREADABLE,
    NO_RESULT,
    CANCELLED,
}

class OnDeviceVision(context: Context) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private var textRecognizer: TextRecognizer? = null
    private val recognizer: TextRecognizer
        get() = textRecognizer ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .also { textRecognizer = it }
    private val barcodeScanner by lazy(LazyThreadSafetyMode.NONE) {
        GmsBarcodeScanning.getClient(
            applicationContext,
            GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_CODABAR,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_DATA_MATRIX,
                    Barcode.FORMAT_PDF417,
                    Barcode.FORMAT_AZTEC,
                )
                .enableAutoZoom()
                .build(),
        )
    }

    fun scanBarcode(
        onResult: (String) -> Unit,
        onFailure: (VisionFailure) -> Unit,
    ) {
        barcodeScanner.startScan()
            .addOnSuccessListener { barcode ->
                BarcodeResultValidator.normalize(barcode.rawValue, barcode.format)?.let(onResult)
                    ?: onFailure(VisionFailure.NO_RESULT)
            }
            .addOnCanceledListener { onFailure(VisionFailure.CANCELLED) }
            .addOnFailureListener { onFailure(VisionFailure.MODEL_UNAVAILABLE) }
    }

    fun recognizeText(
        uri: Uri,
        onResult: (String) -> Unit,
        onFailure: (VisionFailure) -> Unit,
    ) {
        val image = runCatching { InputImage.fromFilePath(applicationContext, uri) }
            .getOrElse {
                onFailure(VisionFailure.IMAGE_UNREADABLE)
                return
            }
        process(image, onResult, onFailure)
    }

    fun recognizeText(
        bitmap: Bitmap,
        onResult: (String) -> Unit,
        onFailure: (VisionFailure) -> Unit,
    ) = process(InputImage.fromBitmap(bitmap, 0), onResult, onFailure)

    private fun process(
        image: InputImage,
        onResult: (String) -> Unit,
        onFailure: (VisionFailure) -> Unit,
    ) {
        recognizer.process(image)
            .addOnSuccessListener { result ->
                result.text.trim().takeIf(String::isNotBlank)?.let(onResult)
                    ?: onFailure(VisionFailure.NO_RESULT)
            }
            .addOnFailureListener { onFailure(VisionFailure.MODEL_UNAVAILABLE) }
    }

    override fun close() {
        textRecognizer?.close()
        textRecognizer = null
    }

}

internal object BarcodeResultValidator {
    fun normalize(rawValue: String?, format: Int): String? {
        val value = rawValue?.trim()?.takeIf { it.isNotEmpty() && it.length <= 256 }
            ?: return null
        if (value.any(Char::isISOControl)) return null
        return when (format) {
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_UPC_A -> value.takeIf(::hasValidGtinCheckDigit)
            // UPC-E uses its expanded UPC-A representation for checksum validation; the
            // scanner has already decoded that symbology, so avoid applying GTIN-8 math.
            else -> value
        }
    }

    private fun hasValidGtinCheckDigit(value: String): Boolean {
        if (value.length !in setOf(8, 12, 13, 14) || !value.all(Char::isDigit)) return false
        val expected = value.last().digitToInt()
        val sum = value.dropLast(1).reversed().mapIndexed { index, character ->
            character.digitToInt() * if (index % 2 == 0) 3 else 1
        }.sum()
        return (10 - (sum % 10)) % 10 == expected
    }
}

