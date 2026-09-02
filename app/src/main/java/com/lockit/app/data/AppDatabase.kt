package com.lockit.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromDuration(value: TaskDuration): String = value.name
    @TypeConverter
    fun toDuration(value: String): TaskDuration = TaskDuration.valueOf(value)

    @TypeConverter
    fun fromRecurrence(value: TaskRecurrence): String = value.name
    @TypeConverter
    fun toRecurrence(value: String): TaskRecurrence = TaskRecurrence.valueOf(value)

    @TypeConverter
    fun fromVerification(value: VerificationMethod): String = value.name
    @TypeConverter
    fun toVerification(value: String): VerificationMethod = VerificationMethod.valueOf(value)
}

@Database(
    entities = [Task::class, LockedApp::class, TokenState::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun tokenDao(): TokenDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lockit.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
