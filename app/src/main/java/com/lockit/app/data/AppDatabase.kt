package com.lockin.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter fun fromDuration(v: TaskDuration): String = v.name
    @TypeConverter fun toDuration(v: String): TaskDuration = TaskDuration.valueOf(v)

    @TypeConverter fun fromRecurrence(v: TaskRecurrence): String = v.name
    @TypeConverter fun toRecurrence(v: String): TaskRecurrence = TaskRecurrence.valueOf(v)

    @TypeConverter fun fromVerification(v: VerificationMethod): String = v.name
    @TypeConverter fun toVerification(v: String): VerificationMethod = VerificationMethod.valueOf(v)
}

@Database(
    entities = [Task::class, LockedApp::class, TokenState::class, TaskAppLink::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun tokenDao(): TokenDao
    abstract fun taskAppLinkDao(): TaskAppLinkDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * v2 → v3: adds the optional scheduledTime column ("HH:mm" or "").
         * Existing rows get an empty string, which means "no time set" — no
         * behaviour change for anything already in the database.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE tasks ADD COLUMN scheduledTime TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lockin.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    // Safety net for anything older than v2 (v1 and v2 never
                    // held reliable data, so wiping is acceptable).
                    .fallbackToDestructiveMigrationFrom(1)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
