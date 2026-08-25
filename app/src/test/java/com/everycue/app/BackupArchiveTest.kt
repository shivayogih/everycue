package com.everycue.app

import com.everycue.feature.pack.PackData
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupArchiveTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `archive round trip preserves attachment bytes and verified metadata`() {
        val sourceFile = temporaryFolder.newFile("passport.pdf").apply {
            writeBytes("verified renewal document".toByteArray())
        }
        val source = attachmentSource(sourceFile)
        val output = ByteArrayOutputStream()

        val finalized = BackupArchiveIO.write(output, payload(), listOf(source))
        val contents = BackupArchiveIO.read(
            ByteArrayInputStream(output.toByteArray()),
            temporaryFolder.newFolder("restore"),
        )

        assertTrue(contents.isAttachmentArchive)
        assertEquals(finalized, contents.payload)
        assertEquals(64, finalized.renewalAttachments.single().sha256.length)
        assertArrayEquals(sourceFile.readBytes(), contents.stagedFilesByAttachmentId.getValue(ATTACHMENT_ID).readBytes())
    }

    @Test
    fun `legacy JSON is explicitly distinguished from an attachment archive`() {
        val legacyPayload = payload().copy(formatVersion = 1)

        val contents = BackupArchiveIO.readLegacyJson(
            Json.encodeToString(legacyPayload).byteInputStream(),
        )

        assertEquals(legacyPayload, contents.payload)
        assertTrue(!contents.isAttachmentArchive)
        assertTrue(contents.stagedFilesByAttachmentId.isEmpty())
    }

    @Test
    fun `checksum mismatch fails before returning staged content`() {
        val bytes = "tampered document".toByteArray()
        val metadata = attachmentSource(temporaryFolder.newFile("source.pdf")).metadata.copy(
            sizeBytes = bytes.size.toLong(),
            archiveEntry = "attachments/$ATTACHMENT_ID.bin",
            sha256 = "0".repeat(64),
        )
        val badPayload = payload().copy(renewalAttachments = listOf(metadata))
        val archive = manualArchive(badPayload, metadata.archiveEntry, bytes)
        val restoreDirectory = temporaryFolder.newFolder("bad-checksum")

        val error = runCatching {
            BackupArchiveIO.read(ByteArrayInputStream(archive), restoreDirectory)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(!restoreDirectory.exists())
    }

    @Test
    fun `unsafe archive paths are rejected without writing outside staging`() {
        val metadata = attachmentSource(temporaryFolder.newFile("source-2.pdf")).metadata.copy(
            sizeBytes = 1,
            archiveEntry = "../outside.bin",
            sha256 = "0".repeat(64),
        )
        val badPayload = payload().copy(renewalAttachments = listOf(metadata))
        val restoreDirectory = temporaryFolder.newFolder("bad-path")

        val error = runCatching {
            BackupArchiveIO.read(
                ByteArrayInputStream(manualArchive(badPayload, metadata.archiveEntry, byteArrayOf(1))),
                restoreDirectory,
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(!restoreDirectory.exists())
        assertTrue(!temporaryFolder.root.resolve("outside.bin").exists())
    }

    private fun payload() = EveryCueBackup(
        exportedAtMillis = 100,
        trackItems = emptyList(),
        trackEvents = emptyList(),
        renewals = listOf(
            RenewalBackup(
                id = RENEWAL_ID,
                title = "Passport",
                type = "DOCUMENT",
                dueEpochDay = 22_000,
                reminderDays = 30,
                provider = "Authority",
                referenceNumber = "P123",
                notes = "",
                lastRenewedEpochDay = null,
                lifecycleStatus = "ACTIVE",
                createdAtMillis = 1,
                updatedAtMillis = 2,
            ),
        ),
        renewalEvents = emptyList(),
        pack = PackData(),
        settings = AppSettings(),
    )

    private fun attachmentSource(file: java.io.File) = BackupAttachmentSource(
        metadata = RenewalAttachmentBackup(
            id = ATTACHMENT_ID,
            renewalId = RENEWAL_ID,
            displayName = "passport.pdf",
            mimeType = "application/pdf",
            sizeBytes = file.length().coerceAtLeast(1),
            createdAtMillis = 3,
            source = "DOCUMENT",
        ),
        file = file,
    )

    private fun manualArchive(payload: EveryCueBackup, entryName: String, bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(Json { encodeDefaults = true }.encodeToString(payload).toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(entryName))
            zip.write(bytes)
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    private companion object {
        const val RENEWAL_ID = "renewal-1"
        const val ATTACHMENT_ID = "11111111-1111-1111-1111-111111111111"
    }
}
