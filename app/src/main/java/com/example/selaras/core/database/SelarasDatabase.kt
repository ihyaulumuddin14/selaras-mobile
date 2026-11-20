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
    version = 3,
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
         * Builds encrypted Room database backed by SQLCipher.
         *
         * Order of execution:
         * 1. Loads SQLCipher native library via [System.loadLibrary].
         * 2. Creates [net.sqlcipher.database.SupportFactory] using DEK passphrase bytes.
         * 3. Configures Room database with [openHelperFactory].
         */
        fun buildEncrypted(
            context: Context,
            passphrase: ByteArray,
            databaseName: String = DATABASE_NAME
        ): SelarasDatabase {
            System.loadLibrary("sqlcipher")
            val factory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                SelarasDatabase::class.java,
                databaseName
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration(true)
                .build()
        }

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
