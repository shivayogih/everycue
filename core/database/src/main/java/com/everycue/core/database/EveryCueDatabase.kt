package com.everycue.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackItemEntity::class,
        TrackEventEntity::class,
        RenewalEntity::class,
        RenewalEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class EveryCueDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun renewalDao(): RenewalDao

    companion object {
        @Volatile
        private var instance: EveryCueDatabase? = null

        fun getInstance(context: Context): EveryCueDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                EveryCueDatabase::class.java,
                "everycue.db",
            ).build().also { instance = it }
        }
    }
}
