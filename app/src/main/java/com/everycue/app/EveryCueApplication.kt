package com.everycue.app

import android.app.Application
import com.everycue.core.database.EveryCueDatabase
import com.everycue.feature.pack.PackRepository
import com.everycue.feature.renew.RenewRepository
import com.everycue.feature.track.TrackRepository

class EveryCueApplication : Application() {
    val database: EveryCueDatabase by lazy { EveryCueDatabase.getInstance(this) }
    val trackRepository: TrackRepository by lazy { TrackRepository(database) }
    val packRepository: PackRepository by lazy { PackRepository(this) }
    val renewRepository: RenewRepository by lazy { RenewRepository(database) }
}
