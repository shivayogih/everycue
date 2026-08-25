package com.everycue.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

enum class VisionFailure {
    MODEL_UNAVAILABLE,
    IMAGE_UNREADABLE,
    NO_RESULT,
    CANCELLED,
}

class OnDeviceVision(context: Context) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodeScanner = GmsBarcodeScanning.getClient(
        applicationContext,
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX,
            )
            .enableAutoZoom()
            .build(),
    )

    fun scanBarcode(
        onResult: (String) -> Unit,
        onFailure: (VisionFailure) -> Unit,
    ) {
        barcodeScanner.startScan()
            .addOnSuccessListener { barcode ->
                barcode.rawValue?.trim()?.takeIf(String::isNotBlank)?.let(onResult)
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
        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                result.text.trim().takeIf(String::isNotBlank)?.let(onResult)
                    ?: onFailure(VisionFailure.NO_RESULT)
            }
            .addOnFailureListener { onFailure(VisionFailure.MODEL_UNAVAILABLE) }
    }

    override fun close() {
        textRecognizer.close()
    }
}
