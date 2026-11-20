package com.example.selaras.core.di

import android.content.Context
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.core.security.KeyManager
import com.example.selaras.data.repository.BreathingRepositoryImpl
import com.example.selaras.domain.repository.BreathingRepository

/**
 * The beginning of the team's planned manual AppContainer (ADR-011).
 * Other feature repositories can be registered here when their screens are ready.
 */
interface AppContainer {
    val breathingRepository: BreathingRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    private val database: SelarasDatabase by lazy {
        SelarasDatabase.buildEncrypted(
            context = appContext,
            passphrase = KeyManager(appContext).getOrCreateDatabaseKey()
        )
    }

    override val breathingRepository: BreathingRepository by lazy {
        BreathingRepositoryImpl(database.customBreathingPatternDao())
    }
}
