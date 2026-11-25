package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class LessonTypeConverters {
    @TypeConverter
    fun fromLessonType(value: LessonType): String = value.name

    @TypeConverter
    fun toLessonType(value: String): LessonType =
        runCatching { LessonType.valueOf(value) }.getOrDefault(LessonType.DOCUMENT)
}

@Database(
    entities = [SubjectEntity::class, LessonEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(LessonTypeConverters::class)
abstract class LessonVaultDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun lessonDao(): LessonDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lessons ADD COLUMN labels TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: LessonVaultDatabase? = null

        fun getInstance(context: Context): LessonVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LessonVaultDatabase::class.java,
                    "lesson_vault.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
