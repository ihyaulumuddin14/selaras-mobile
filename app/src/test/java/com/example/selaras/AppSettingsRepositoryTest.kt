package com.example.selaras

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.data.datasource.local.AppSettingsLocalDataSource
import com.example.selaras.data.repository.AppSettingsRepositoryImpl
import com.example.selaras.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppSettingsRepositoryTest {

    private lateinit var database: SelarasDatabase
    private lateinit var localDataSource: AppSettingsLocalDataSource
    private lateinit var repository: AppSettingsRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SelarasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        localDataSource = AppSettingsLocalDataSource(database.appSettingsDao())
        repository = AppSettingsRepositoryImpl(localDataSource)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInitializeIfFirstLaunchCreatesSingletonSettings() = runTest {
        val settings = repository.initializeIfFirstLaunch().getOrThrow()
        assertNotNull(settings)
        assertEquals(1L, settings.id)
        assertFalse(settings.onboardingCompleted)
        assertFalse(settings.isConsentAccepted)
        assertNull(settings.consentAcceptedAt)

        // Second call should return existing record without overwriting
        val second = repository.initializeIfFirstLaunch().getOrThrow()
        assertEquals(settings.firstActivatedAt, second.firstActivatedAt)
    }

    @Test
    fun testAcceptConsentUpdatesTimestampAndFlag() = runTest {
        repository.initializeIfFirstLaunch()
        val acceptResult = repository.acceptConsent()
        assertTrue(acceptResult.isSuccess)

        val updated = repository.getSettings()
        assertNotNull(updated)
        assertTrue(updated!!.isConsentAccepted)
        assertNotNull(updated.consentAcceptedAt)
    }

    @Test
    fun testCompleteOnboardingUpdatesFlag() = runTest {
        repository.initializeIfFirstLaunch()
        repository.acceptConsent()
        val completeResult = repository.completeOnboarding()
        assertTrue(completeResult.isSuccess)

        val updated = repository.getSettingsFlow().first()
        assertNotNull(updated)
        assertTrue(updated!!.onboardingCompleted)
    }
}
