package com.everycue.core.attachments

import android.net.Uri
import java.io.File
import java.io.InputStream

enum class AttachmentOwnerType {
    TRACK_ITEM,
    RENEWAL,
    TRIP,
}

enum class AttachmentSource {
    CAMERA,
    GALLERY,
    DOCUMENT,
}

data class AttachmentOwner(
    val type: AttachmentOwnerType,
    val id: String,
) {
    init {
        require(id.isNotBlank())
    }
}

data class LocalAttachment(
    val id: String,
    val owner: AttachmentOwner,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    /** App-private path relative to the attachment root; never an external source URI. */
    val localReference: String,
    val createdAtMillis: Long,
    val source: AttachmentSource,
)

data class AttachmentImportRequest(
    val owner: AttachmentOwner,
    val sourceUri: Uri,
    val source: AttachmentSource,
    val displayNameHint: String? = null,
    val mimeTypeHint: String? = null,
)

enum class AttachmentError {
    SOURCE_UNAVAILABLE,
    EMPTY_FILE,
    FILE_TOO_LARGE,
    UNSUPPORTED_TYPE,
    INVALID_REFERENCE,
    COPY_FAILED,
}

class AttachmentException(
    val error: AttachmentError,
    cause: Throwable? = null,
) : Exception(error.name, cause)

interface AttachmentStore {
    suspend fun import(request: AttachmentImportRequest): LocalAttachment
    suspend fun remove(attachment: LocalAttachment): Boolean
    suspend fun open(attachment: LocalAttachment): InputStream
    suspend fun cleanupTemporaryCaptures(olderThanMillis: Long): Int
    /** Returns only a validated app-owned file for FileProvider; never an arbitrary path. */
    fun contentFile(attachment: LocalAttachment): File
    fun createTemporaryCaptureFile(): File
    fun removeTemporaryCapture(file: File): Boolean
}

