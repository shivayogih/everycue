package com.everycue.app

import com.everycue.core.attachments.AttachmentPolicy
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal data class BackupAttachmentSource(
    val metadata: RenewalAttachmentBackup,
    val file: File,
)

internal data class BackupArchiveContents(
    val payload: EveryCueBackup,
    val stagedFilesByAttachmentId: Map<String, File>,
    val isAttachmentArchive: Boolean,
)

/** Streaming, bounded archive codec. It does not mutate application state. */
internal object BackupArchiveIO {
    private const val MANIFEST_ENTRY = "manifest.json"
    private const val MAX_MANIFEST_BYTES = 5 * 1024 * 1024
    private const val MAX_ATTACHMENT_COUNT = 2_000
    private const val MAX_TOTAL_ATTACHMENT_BYTES = 2L * 1024L * 1024L * 1024L
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true; prettyPrint = true }
    private val safeId = Regex("[A-Za-z0-9_-]{1,100}")
    private val sha256 = Regex("[a-f0-9]{64}")

    fun write(
        output: OutputStream,
        payload: EveryCueBackup,
        sources: List<BackupAttachmentSource>,
    ): EveryCueBackup {
        require(sources.size <= MAX_ATTACHMENT_COUNT) { "Backup contains too many attachments." }
        require(sources.map { it.metadata.id }.distinct().size == sources.size) {
            "Backup contains duplicate attachment IDs."
        }

        var totalBytes = 0L
        val finalized = sources.map { source ->
            require(source.file.isFile) { "An attachment file is unavailable." }
            val (size, digest) = digest(source.file)
            AttachmentPolicy.validateSize(size)?.let { error("Invalid attachment size: $it") }
            totalBytes = Math.addExact(totalBytes, size)
            require(totalBytes <= MAX_TOTAL_ATTACHMENT_BYTES) { "Backup attachments are too large." }
            source.metadata.copy(
                sizeBytes = size,
                archiveEntry = archiveEntry(source.metadata.id),
                sha256 = digest,
            )
        }
        val finalizedPayload = payload.copy(renewalAttachments = finalized)
        validateAttachmentManifest(finalizedPayload)
        val manifestBytes = json.encodeToString(finalizedPayload).toByteArray(Charsets.UTF_8)
        require(manifestBytes.size <= MAX_MANIFEST_BYTES) { "Backup manifest is too large." }

        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifestBytes)
            zip.closeEntry()

            finalized.zip(sources).forEach { (metadata, source) ->
                zip.putNextEntry(ZipEntry(metadata.archiveEntry))
                writeVerifiedAttachment(zip, source.file, metadata)
                zip.closeEntry()
            }
        }
        return finalizedPayload
    }

    fun readLegacyJson(input: InputStream): BackupArchiveContents {
        val payload = json.decodeFromString<EveryCueBackup>(
            readBounded(input, MAX_MANIFEST_BYTES).toString(Charsets.UTF_8),
        )
        return BackupArchiveContents(
            payload = payload,
            stagedFilesByAttachmentId = emptyMap(),
            isAttachmentArchive = false,
        )
    }

    fun read(input: InputStream, stagingDirectory: File): BackupArchiveContents {
        require(!stagingDirectory.exists() || stagingDirectory.listFiles().isNullOrEmpty()) {
            "Backup staging directory is not empty."
        }
        require(stagingDirectory.exists() || stagingDirectory.mkdirs()) {
            "Backup staging directory could not be created."
        }

        try {
            ZipInputStream(input.buffered()).use { zip ->
                val manifestEntry = zip.nextEntry ?: error("Backup archive is empty.")
                require(!manifestEntry.isDirectory && manifestEntry.name == MANIFEST_ENTRY) {
                    "Backup manifest must be the first archive entry."
                }
                val manifest = readBounded(zip, MAX_MANIFEST_BYTES).toString(Charsets.UTF_8)
                zip.closeEntry()
                val payload = json.decodeFromString<EveryCueBackup>(manifest)
                require(payload.formatVersion >= ATTACHMENT_ARCHIVE_BACKUP_VERSION) {
                    "Attachment archives require backup format version $ATTACHMENT_ARCHIVE_BACKUP_VERSION."
                }
                validateAttachmentManifest(payload)

                val expectedByEntry = payload.renewalAttachments.associateBy(RenewalAttachmentBackup::archiveEntry)
                val staged = linkedMapOf<String, File>()
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(!entry.isDirectory) { "Unexpected backup directory entry." }
                    val metadata = expectedByEntry[entry.name] ?: error("Unexpected backup attachment entry.")
                    require(metadata.id !in staged) { "Duplicate backup attachment entry." }
                    val destination = File(stagingDirectory, "${metadata.id}.staged")
                    val digest = MessageDigest.getInstance("SHA-256")
                    var copied = 0L
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            copied += count
                            require(copied <= metadata.sizeBytes && copied <= AttachmentPolicy.MAX_ATTACHMENT_BYTES) {
                                "Backup attachment exceeds its declared size."
                            }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                    require(copied == metadata.sizeBytes) { "Backup attachment size does not match its manifest." }
                    require(digest.digest().toHex() == metadata.sha256) {
                        "Backup attachment checksum does not match its manifest."
                    }
                    staged[metadata.id] = destination
                    zip.closeEntry()
                }
                require(staged.size == payload.renewalAttachments.size) {
                    "Backup archive is missing attachment files."
                }
                return BackupArchiveContents(
                    payload = payload,
                    stagedFilesByAttachmentId = staged,
                    isAttachmentArchive = true,
                )
            }
        } catch (error: Throwable) {
            stagingDirectory.deleteRecursively()
            throw error
        }
    }

    private fun validateAttachmentManifest(payload: EveryCueBackup) {
        val attachments = payload.renewalAttachments
        require(attachments.size <= MAX_ATTACHMENT_COUNT) { "Backup contains too many attachments." }
        require(attachments.distinctBy { it.id }.size == attachments.size) {
            "Backup contains duplicate attachment IDs."
        }
        require(attachments.distinctBy { it.archiveEntry }.size == attachments.size) {
            "Backup contains duplicate attachment paths."
        }
        val renewalIds = payload.renewals.mapTo(hashSetOf(), RenewalBackup::id)
        var totalBytes = 0L
        attachments.forEach { attachment ->
            require(attachment.id.matches(safeId)) { "Backup contains an invalid attachment ID." }
            require(attachment.renewalId in renewalIds) { "Backup attachment owner is missing." }
            require(attachment.archiveEntry == archiveEntry(attachment.id)) {
                "Backup contains an invalid attachment path."
            }
            require(attachment.sha256.matches(sha256)) { "Backup contains an invalid checksum." }
            require(AttachmentPolicy.isSupportedMimeType(attachment.mimeType)) {
                "Backup contains an unsupported attachment type."
            }
            require(AttachmentPolicy.validateSize(attachment.sizeBytes) == null) {
                "Backup contains an invalid attachment size."
            }
            require(attachment.displayName.length in 1..120 && attachment.displayName.none(Char::isISOControl)) {
                "Backup contains an invalid attachment name."
            }
            totalBytes = Math.addExact(totalBytes, attachment.sizeBytes)
            require(totalBytes <= MAX_TOTAL_ATTACHMENT_BYTES) { "Backup attachments are too large." }
        }
    }

    private fun archiveEntry(id: String): String = "attachments/$id.bin"

    private fun digest(file: File): Pair<Long, String> {
        val digest = MessageDigest.getInstance("SHA-256")
        var size = 0L
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                size += count
                require(size <= AttachmentPolicy.MAX_ATTACHMENT_BYTES) { "Attachment is too large." }
                digest.update(buffer, 0, count)
            }
        }
        return size to digest.digest().toHex()
    }

    private fun writeVerifiedAttachment(
        output: OutputStream,
        file: File,
        metadata: RenewalAttachmentBackup,
    ) {
        val digest = MessageDigest.getInstance("SHA-256")
        var size = 0L
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                size += count
                require(size <= metadata.sizeBytes && size <= AttachmentPolicy.MAX_ATTACHMENT_BYTES) {
                    "Attachment changed while the backup was being created."
                }
                digest.update(buffer, 0, count)
                output.write(buffer, 0, count)
            }
        }
        require(size == metadata.sizeBytes && digest.digest().toHex() == metadata.sha256) {
            "Attachment changed while the backup was being created."
        }
    }

    private fun readBounded(input: InputStream, maximumBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maximumBytes) { "Backup manifest is too large." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }
}
