package com.example.selaras.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.selaras.core.database.converter.DateConverter
import com.example.selaras.core.database.converter.ScreeningConverter
import com.example.selaras.core.database.dao.AppSettingsDao
import com.example.selaras.core.database.dao.CheckInDao
import com.example.selaras.core.database.dao.CounselorSummaryDao
import com.example.selaras.core.database.dao.CustomBreathingPatternDao
import com.example.selaras.core.database.dao.ScreeningAnswerDao
import com.example.selaras.core.database.dao.ScreeningResultDao
import com.example.selaras.data.local.entity.AppSettingsEntity
import com.example.selaras.data.local.entity.CheckInEntity
import com.example.selaras.data.local.entity.CounselorSummaryEntity
import com.example.selaras.data.local.entity.CustomBreathingPatternEntity
import com.example.selaras.data.local.entity.ScreeningAnswerEntity
import com.example.selaras.data.local.entity.ScreeningResultEntity

@Database(
    entities = [
        AppSettingsEntity::class,
        CheckInEntity::class,
        ScreeningResultEntity::class,
        ScreeningAnswerEntity::class,
        CustomBreathingPatternEntity::class,
        CounselorSummaryEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(
    DateConverter::class,
    ScreeningConverter::class
)
abstract class SelarasDatabase : RoomDatabase() {

    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun checkInDao(): CheckInDao
    abstract fun screeningResultDao(): ScreeningResultDao
    abstract fun screeningAnswerDao(): ScreeningAnswerDao
    abstract fun customBreathingPatternDao(): CustomBreathingPatternDao
    abstract fun counselorSummaryDao(): CounselorSummaryDao

    companion object {
        const val DATABASE_NAME = "selaras.db"

        /**
         * Standard unencrypted database builder (e.g. for in-memory tests).
         */
        fun buildInMemory(context: Context): SelarasDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                SelarasDatabase::class.java
            ).build()
        }
    }
}
