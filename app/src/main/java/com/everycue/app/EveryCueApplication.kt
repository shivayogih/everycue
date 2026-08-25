package com.everycue.app

import android.app.Application
import com.everycue.core.attachments.AppPrivateAttachmentStore
import com.everycue.core.database.EveryCueDatabase
import com.everycue.core.security.AndroidKeystoreTextCipher
import com.everycue.feature.pack.PackRepository
import com.everycue.feature.renew.RenewRepository
import com.everycue.feature.track.TrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class EveryCueApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val textCipher by lazy { AndroidKeystoreTextCipher() }
    val database: EveryCueDatabase by lazy { EveryCueDatabase.getInstance(this) }
    val attachmentStore by lazy { AppPrivateAttachmentStore(this) }
    val trackRepository: TrackRepository by lazy { TrackRepository(database, textCipher) }
    val packRepository: PackRepository by lazy { PackRepository(this, textCipher) }
    val renewRepository: RenewRepository by lazy { RenewRepository(database, textCipher, attachmentStore) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val userProfileRepository: LocalUserProfileRepository by lazy { LocalUserProfileRepository(this, textCipher) }
    val backupRepository: BackupRepository by lazy {
        BackupRepository(
            this,
            database,
            packRepository,
            settingsRepository,
            userProfileRepository,
            textCipher,
            attachmentStore,
        )
    }

    override fun onCreate() {
        super.onCreate()
        if (!DeviceSecurityGuard.isAccessAllowed()) return
        createReminderChannel(this)
        applicationScope.launch {
            settingsRepository.settings.collect { ReminderScheduler.update(this@EveryCueApplication, it) }
        }
        applicationScope.launch {
            combine(trackRepository.items, packRepository.data, renewRepository.renewals) { _, _, _ -> Unit }
                .collect { EveryCueWidgetProvider.updateAll(this@EveryCueApplication) }
        }
    }
}

