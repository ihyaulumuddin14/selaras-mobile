package com.example.selaras.core.di

import android.content.Context
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.core.security.KeyManager
import com.example.selaras.core.security.PinManager
import com.example.selaras.core.security.RecoveryManager
import com.example.selaras.data.datasource.local.AppSettingsLocalDataSource
import com.example.selaras.data.datasource.local.BreathingLocalDataSource
import com.example.selaras.data.datasource.local.CheckInLocalDataSource
import com.example.selaras.data.datasource.remote.FakeBreathingRemoteDataSource
import com.example.selaras.data.datasource.remote.FakeCheckInRemoteDataSource
import com.example.selaras.data.repository.AppSettingsRepositoryImpl
import com.example.selaras.data.repository.BreathingRepositoryImpl
import com.example.selaras.data.repository.CheckInRepositoryImpl
import com.example.selaras.data.repository.ScreeningRepositoryImpl
import com.example.selaras.data.repository.SecurityRepositoryImpl
import com.example.selaras.data.sync.BreathingSyncManager
import com.example.selaras.data.sync.CheckInSyncManager
import com.example.selaras.domain.repository.AppSettingsRepository
import com.example.selaras.domain.repository.BreathingRepository
import com.example.selaras.domain.repository.CheckInRepository
import com.example.selaras.domain.repository.ScreeningRepository
import com.example.selaras.domain.repository.SecurityRepository

/**
 * Manual Dependency Injection container (ADR-011).
 */
interface AppContainer {
    val database: SelarasDatabase
    val keyManager: KeyManager
    val pinManager: PinManager
    val recoveryManager: RecoveryManager
    val securityRepository: SecurityRepository
    val breathingRepository: BreathingRepository
    val checkInRepository: CheckInRepository
    val screeningRepository: ScreeningRepository
    val checkInSyncManager: CheckInSyncManager
    val breathingSyncManager: BreathingSyncManager
    val appSettingsRepository: AppSettingsRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val keyManager: KeyManager by lazy {
        KeyManager(appContext)
    }

    override val pinManager: PinManager by lazy {
        PinManager(appContext)
    }

    override val recoveryManager: RecoveryManager by lazy {
        RecoveryManager(appContext)
    }

    override val securityRepository: SecurityRepository by lazy {
        SecurityRepositoryImpl(
            pinManager = pinManager,
            recoveryManager = recoveryManager,
            keyManager = keyManager
        )
    }

    override val database: SelarasDatabase by lazy {
        val passphrase = keyManager.getOrCreateDatabaseKey()
        SelarasDatabase.buildEncrypted(
            context = appContext,
            passphrase = passphrase
        )
    }

    override val breathingRepository: BreathingRepository by lazy {
        val localDataSource = BreathingLocalDataSource(database.customBreathingPatternDao())
        BreathingRepositoryImpl(localDataSource)
    }

    override val checkInRepository: CheckInRepository by lazy {
        val localDataSource = CheckInLocalDataSource(database.checkInDao())
        CheckInRepositoryImpl(localDataSource)
    }

    override val screeningRepository: ScreeningRepository by lazy {
        ScreeningRepositoryImpl(
            database = database,
            screeningResultDao = database.screeningResultDao(),
            screeningAnswerDao = database.screeningAnswerDao()
        )
    }

    override val checkInSyncManager: CheckInSyncManager by lazy {
        val localDataSource = CheckInLocalDataSource(database.checkInDao())
        val remoteDataSource = FakeCheckInRemoteDataSource()
        CheckInSyncManager(localDataSource, remoteDataSource)
    }

    override val breathingSyncManager: BreathingSyncManager by lazy {
        val localDataSource = BreathingLocalDataSource(database.customBreathingPatternDao())
        val remoteDataSource = FakeBreathingRemoteDataSource()
        BreathingSyncManager(localDataSource, remoteDataSource)
    }

    override val appSettingsRepository: AppSettingsRepository by lazy {
        val localDataSource = AppSettingsLocalDataSource(database.appSettingsDao())
        AppSettingsRepositoryImpl(localDataSource)
    }
}
