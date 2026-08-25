package com.everycue.core.attachments

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Copies user-selected content into app-private storage. Removing an attachment only removes the
 * app-owned copy; the user's gallery/document original is never modified.
 */
class AppPrivateAttachmentStore(
    context: Context,
) : AttachmentStore {
    private val applicationContext = context.applicationContext
    private val attachmentRoot = File(applicationContext.filesDir, ATTACHMENT_DIRECTORY)
    private val captureRoot = File(applicationContext.cacheDir, CAPTURE_DIRECTORY)

    override suspend fun import(request: AttachmentImportRequest): LocalAttachment = onIoThread {
        val resolver = applicationContext.contentResolver
        val metadata = resolver.queryMetadata(request.sourceUri)
        val mimeType = AttachmentPolicy.normalizeMimeType(
            resolver.getType(request.sourceUri) ?: request.mimeTypeHint,
        ) ?: throw AttachmentException(AttachmentError.UNSUPPORTED_TYPE)
        if (!AttachmentPolicy.isSupportedMimeType(mimeType)) {
            throw AttachmentException(AttachmentError.UNSUPPORTED_TYPE)
        }
        metadata.size?.let { size ->
            AttachmentPolicy.validateSize(size)?.let { throw AttachmentException(it) }
        }

        val id = UUID.randomUUID().toString()
        val ownerDirectory = ownerDirectory(request.owner).apply { mkdirs() }
        val relativeReference = listOf(
            request.owner.type.name.lowercase(),
            safePathSegment(request.owner.id),
            id + AttachmentPolicy.extensionFor(mimeType),
        ).joinToString("/")
        val destination = resolveOwnedPath(relativeReference)
        val staging = File(ownerDirectory, ".$id.partial")

        try {
            val source = resolver.openInputStream(request.sourceUri)
                ?: throw AttachmentException(AttachmentError.SOURCE_UNAVAILABLE)
            val copied = source.use { input ->
                FileOutputStream(staging).use { output -> copyBounded(input, output) }
            }
            AttachmentPolicy.validateSize(copied)?.let { throw AttachmentException(it) }
            if (!staging.renameTo(destination)) {
                throw AttachmentException(AttachmentError.COPY_FAILED)
            }
            LocalAttachment(
                id = id,
                owner = request.owner,
                displayName = AttachmentPolicy.safeDisplayName(
                    metadata.displayName ?: request.displayNameHint,
                    fallback = id + AttachmentPolicy.extensionFor(mimeType),
                ),
                mimeType = mimeType,
                sizeBytes = copied,
                localReference = relativeReference,
                createdAtMillis = System.currentTimeMillis(),
                source = request.source,
            )
        } catch (error: AttachmentException) {
            staging.delete()
            throw error
        } catch (error: Exception) {
            staging.delete()
            throw AttachmentException(AttachmentError.COPY_FAILED, error)
        }
    }

    override suspend fun remove(attachment: LocalAttachment): Boolean = onIoThread {
        resolveOwnedPath(attachment.localReference).delete()
    }

    override suspend fun open(attachment: LocalAttachment): InputStream = onIoThread {
        val file = resolveOwnedPath(attachment.localReference)
        if (!file.isFile) throw AttachmentException(AttachmentError.SOURCE_UNAVAILABLE)
        FileInputStream(file)
    }

    override suspend fun cleanupTemporaryCaptures(olderThanMillis: Long): Int = onIoThread {
        if (!captureRoot.exists()) return@onIoThread 0
        val now = System.currentTimeMillis()
        captureRoot.listFiles().orEmpty().count { file ->
            file.isFile && now - file.lastModified() >= olderThanMillis && file.delete()
        }
    }

    private fun ownerDirectory(owner: AttachmentOwner): File = File(
        attachmentRoot,
        "${owner.type.name.lowercase()}/${safePathSegment(owner.id)}",
    )

    private fun resolveOwnedPath(reference: String): File {
        val root = attachmentRoot.canonicalFile
        val candidate = File(root, reference).canonicalFile
        if (candidate == root || !candidate.path.startsWith(root.path + File.separator)) {
            throw AttachmentException(AttachmentError.INVALID_REFERENCE)
        }
        return candidate
    }

    private fun safePathSegment(value: String): String = value
        .replace(Regex("[^A-Za-z0-9_-]"), "_")
        .take(100)
        .ifBlank { throw AttachmentException(AttachmentError.INVALID_REFERENCE) }

    private fun copyBounded(input: InputStream, output: FileOutputStream): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > AttachmentPolicy.MAX_ATTACHMENT_BYTES) {
                throw AttachmentException(AttachmentError.FILE_TOO_LARGE)
            }
            output.write(buffer, 0, read)
        }
        output.fd.sync()
        return total
    }

    private suspend fun <T> onIoThread(block: () -> T): T = suspendCoroutine { continuation ->
        IO_EXECUTOR.execute {
            try {
                continuation.resume(block())
            } catch (error: Throwable) {
                continuation.resumeWithException(error)
            }
        }
    }

    private data class SourceMetadata(
        val displayName: String?,
        val size: Long?,
    )

    private fun android.content.ContentResolver.queryMetadata(uri: Uri): SourceMetadata {
        var cursor: Cursor? = null
        return try {
            cursor = query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            if (cursor?.moveToFirst() == true) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                SourceMetadata(
                    displayName = if (nameIndex >= 0) cursor.getString(nameIndex) else null,
                    size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null,
                )
            } else {
                SourceMetadata(null, null)
            }
        } catch (_: RuntimeException) {
            SourceMetadata(null, null)
        } finally {
            cursor?.close()
        }
    }

    companion object {
        private const val ATTACHMENT_DIRECTORY = "everycue_attachments"
        private const val CAPTURE_DIRECTORY = "everycue_captures"
        private val IO_EXECUTOR = java.util.concurrent.Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "everycue-attachment-io").apply { isDaemon = true }
        }
    }
}
