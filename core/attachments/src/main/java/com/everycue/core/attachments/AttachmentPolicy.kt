package com.everycue.core.attachments

import java.util.Locale

object AttachmentPolicy {
    const val MAX_ATTACHMENT_BYTES: Long = 20L * 1024L * 1024L

    private val supportedMimeTypes = setOf(
        "application/pdf",
        "image/heic",
        "image/heif",
        "image/jpeg",
        "image/png",
        "image/webp",
        "text/plain",
    )

    fun normalizeMimeType(value: String?): String? = value
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.takeIf(String::isNotBlank)

    fun isSupportedMimeType(value: String?): Boolean = normalizeMimeType(value) in supportedMimeTypes

    fun validateSize(sizeBytes: Long): AttachmentError? = when {
        sizeBytes <= 0L -> AttachmentError.EMPTY_FILE
        sizeBytes > MAX_ATTACHMENT_BYTES -> AttachmentError.FILE_TOO_LARGE
        else -> null
    }

    fun safeDisplayName(value: String?, fallback: String): String {
        val cleaned = value
            ?.replace(Regex("[\\p{Cc}\\p{Cf}\\\\/:*?\"<>|]"), "_")
            ?.replace(Regex("\\s+"), " ")
            ?.trim(' ', '.', '_')
            ?.take(120)
            .orEmpty()
        return cleaned.ifBlank { fallback }
    }

    fun extensionFor(mimeType: String): String = when (normalizeMimeType(mimeType)) {
        "application/pdf" -> ".pdf"
        "image/heic" -> ".heic"
        "image/heif" -> ".heif"
        "image/jpeg" -> ".jpg"
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        "text/plain" -> ".txt"
        else -> ""
    }
}
