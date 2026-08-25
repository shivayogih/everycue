package com.everycue.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TrackItemEntity::class,
        TrackEventEntity::class,
        TrackCoachPreferenceEntity::class,
        RenewalEntity::class,
        RenewalEventEntity::class,
        RenewalAttachmentEntity::class,
    ],
    version = 4,
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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE track_items ADD COLUMN barcode TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE track_events ADD COLUMN categorySnapshot TEXT NOT NULL DEFAULT 'OTHER'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS track_coach_preferences (
                        key TEXT NOT NULL,
                        type TEXT NOT NULL,
                        value TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(key)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_track_coach_preferences_type ON track_coach_preferences(type)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS renewal_attachments (
                        id TEXT NOT NULL,
                        renewalId TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        sizeBytes INTEGER NOT NULL,
                        localReference TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_renewal_attachments_renewalId ON renewal_attachments(renewalId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_renewal_attachments_createdAtMillis ON renewal_attachments(createdAtMillis)",
                )
            }
        }
    }
}

