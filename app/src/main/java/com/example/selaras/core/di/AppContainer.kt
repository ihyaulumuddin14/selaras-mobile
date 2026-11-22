
package com.example.selaras.core.di

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.core.security.KeyManager
import com.example.selaras.core.security.PinManager
import com.example.selaras.core.security.RecoveryManager
import com.example.selaras.data.datasource.local.AppSettingsLocalDataSource
import com.example.selaras.data.datasource.local.BreathingLocalDataSource
import com.example.selaras.data.datasource.local.CheckInLocalDataSource
import com.example.selaras.core.security.AuthSessionManager
import com.example.selaras.data.datasource.remote.HttpBreathingRemoteDataSource
import com.example.selaras.data.datasource.remote.HttpCheckInRemoteDataSource
import com.example.selaras.data.datasource.remote.HttpScreeningRemoteDataSource
import com.example.selaras.data.datasource.remote.SelarasApiClient
import com.example.selaras.data.sync.ScreeningSyncManager
import com.example.selaras.data.sync.SyncCoordinator
import com.example.selaras.data.repository.AppSettingsRepositoryImpl
import com.example.selaras.data.repository.BreathingRepositoryImpl
import com.example.selaras.data.repository.CheckInRepositoryImpl
import com.example.selaras.data.repository.CounselorRepositoryImpl
import com.example.selaras.data.repository.ScreeningRepositoryImpl
import com.example.selaras.data.repository.SecurityRepositoryImpl
import com.example.selaras.data.sync.BreathingSyncManager
import com.example.selaras.data.sync.CheckInSyncManager
import com.example.selaras.domain.repository.AppSettingsRepository
import com.example.selaras.domain.repository.BreathingRepository
import com.example.selaras.domain.repository.CheckInRepository
import com.example.selaras.domain.repository.CounselorRepository
import com.example.selaras.domain.repository.ScreeningRepository
import com.example.selaras.domain.repository.SecurityRepository
import com.example.selaras.domain.usecase.screening.CalculatePhq9ScoreUseCase
import com.example.selaras.domain.usecase.screening.CheckCrisisIndicatorUseCase
import com.example.selaras.domain.usecase.screening.ClassifyPhq9ScoreUseCase
import com.example.selaras.domain.usecase.screening.SubmitScreeningUseCase
import com.example.selaras.features.dashboard.DailyCheckInPromptManager
import com.example.selaras.domain.usecase.screening.GetScreeningQuotaUseCase

class ScreeningHandoffStore {
    private val _selectedResultId = MutableStateFlow<Long?>(null)
    val selectedResultId: StateFlow<Long?> = _selectedResultId.asStateFlow()

    fun selectResult(resultId: Long) {
        require(resultId > 0L)
        _selectedResultId.value = resultId
    }

    fun clear() {
        _selectedResultId.value = null
    }
}

interface AppContainer {
    val database: SelarasDatabase
    val keyManager: KeyManager
    val pinManager: PinManager
    val recoveryManager: RecoveryManager
    val securityRepository: SecurityRepository
    val breathingRepository: BreathingRepository
    val checkInRepository: CheckInRepository
    val screeningRepository: ScreeningRepository
    val submitScreeningUseCase: SubmitScreeningUseCase
    val counselorRepository: CounselorRepository
    val checkInSyncManager: CheckInSyncManager
    val breathingSyncManager: BreathingSyncManager
    val screeningSyncManager: ScreeningSyncManager
    val syncCoordinator: SyncCoordinator
    val authSessionManager: AuthSessionManager
    val apiClient: SelarasApiClient
    val appSettingsRepository: AppSettingsRepository
    val dailyCheckInPromptManager: DailyCheckInPromptManager
    val getScreeningQuotaUseCase: GetScreeningQuotaUseCase
    val screeningHandoffStore: ScreeningHandoffStore
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val appContext = context.applicationContext

    override val screeningHandoffStore = ScreeningHandoffStore()

    override val dailyCheckInPromptManager: DailyCheckInPromptManager by lazy {
        DailyCheckInPromptManager.create(appContext)
    }
    override val getScreeningQuotaUseCase: GetScreeningQuotaUseCase by lazy {
        GetScreeningQuotaUseCase(screeningRepository, appSettingsRepository)
    }

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
        val localDataSource = BreathingLocalDataSource(
            database.customBreathingPatternDao()
        )

        BreathingRepositoryImpl(localDataSource)
    }

    override val checkInRepository: CheckInRepository by lazy {
        val localDataSource = CheckInLocalDataSource(
            database.checkInDao()
        )

        CheckInRepositoryImpl(localDataSource)
    }

    override val screeningRepository: ScreeningRepository by lazy {
        ScreeningRepositoryImpl(
            database = database,
            screeningResultDao = database.screeningResultDao(),
            screeningAnswerDao = database.screeningAnswerDao()
        )
    }

    override val submitScreeningUseCase: SubmitScreeningUseCase by lazy {
        SubmitScreeningUseCase(
            screeningRepository = screeningRepository,
            calculatePhq9ScoreUseCase = CalculatePhq9ScoreUseCase(),
            classifyPhq9ScoreUseCase = ClassifyPhq9ScoreUseCase(),
            checkCrisisIndicatorUseCase = CheckCrisisIndicatorUseCase()
        )
    }

    override val counselorRepository: CounselorRepository by lazy {
        CounselorRepositoryImpl(database.counselorSummaryDao())
    }

    override val authSessionManager: AuthSessionManager by lazy {
        AuthSessionManager(appContext)
    }

    override val apiClient: SelarasApiClient by lazy {
        SelarasApiClient()
    }

    override val checkInSyncManager: CheckInSyncManager by lazy {
        val localDataSource = CheckInLocalDataSource(
            database.checkInDao()
        )
        val remoteDataSource = HttpCheckInRemoteDataSource(apiClient, authSessionManager)
        CheckInSyncManager(
            localDataSource,
            remoteDataSource
        )
    }

    override val breathingSyncManager: BreathingSyncManager by lazy {
        val localDataSource = BreathingLocalDataSource(
            database.customBreathingPatternDao()
        )
        val remoteDataSource = HttpBreathingRemoteDataSource(apiClient, authSessionManager)
        BreathingSyncManager(
            localDataSource,
            remoteDataSource
        )
    }

    override val screeningSyncManager: ScreeningSyncManager by lazy {
        ScreeningSyncManager(
            screeningResultDao = database.screeningResultDao(),
            screeningAnswerDao = database.screeningAnswerDao(),
            remoteDataSource = HttpScreeningRemoteDataSource(apiClient, authSessionManager)
        )
    }

    override val syncCoordinator: SyncCoordinator by lazy {
        SyncCoordinator(
            checkInSyncManager = checkInSyncManager,
            breathingSyncManager = breathingSyncManager,
            screeningSyncManager = screeningSyncManager,
            authSessionManager = authSessionManager
        )
    }

    override val appSettingsRepository: AppSettingsRepository by lazy {
        val localDataSource = AppSettingsLocalDataSource(
            database.appSettingsDao()
        )

        AppSettingsRepositoryImpl(localDataSource)
    }
}
