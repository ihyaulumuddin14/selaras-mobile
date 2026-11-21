package com.example.selaras.data.repository

import com.example.selaras.core.security.KeyManager
import com.example.selaras.core.security.PinManager
import com.example.selaras.core.security.RecoveryManager
import com.example.selaras.domain.repository.SecurityRepository

/**
 * Implementation of [SecurityRepository] coordinating [PinManager], [RecoveryManager], and [KeyManager].
 */
class SecurityRepositoryImpl(
    private val pinManager: PinManager,
    private val recoveryManager: RecoveryManager,
    private val keyManager: KeyManager
) : SecurityRepository {

    override fun isPinSet(): Boolean {
        return pinManager.isPinSet()
    }

    override fun isRecoveryConfigured(): Boolean {
        return recoveryManager.isRecoveryConfigured()
    }

    override fun setupPinAndRecovery(pin: String): Result<String> {
        return try {
            if (pin.length != 6 || !pin.all { it.isDigit() }) {
                return Result.failure(IllegalArgumentException("PIN harus berupa 6 digit angka"))
            }

            // 1. Save hashed PIN
            val pinSaved = pinManager.setPin(pin)
            if (!pinSaved) {
                return Result.failure(IllegalStateException("Gagal menyimpan PIN"))
            }

            // 2. Retrieve existing DEK from Keystore
            val currentDek = keyManager.getOrCreateDatabaseKey()

            // 3. Generate recovery code and backup wrapped DEK
            val recoveryCode = recoveryManager.setupRecovery(currentDek)
            Result.success(recoveryCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun verifyPin(pin: String): Boolean {
        return pinManager.verifyPin(pin)
    }

    override fun verifyRecoveryCode(code: String): Boolean {
        return recoveryManager.recoverDatabaseKey(code) != null
    }

    override fun resetPinWithRecovery(recoveryCode: String, newPin: String): Result<Unit> {
        return try {
            if (newPin.length != 6 || !newPin.all { it.isDigit() }) {
                return Result.failure(IllegalArgumentException("PIN baru harus berupa 6 digit angka"))
            }

            // 1. Verify recovery code by attempting to recover DEK
            val recoveredDek = recoveryManager.recoverDatabaseKey(recoveryCode)
                ?: return Result.failure(IllegalArgumentException("Kode pemulihan tidak cocok atau tidak valid"))

            // 2. Ensure Keystore has the valid DEK
            keyManager.saveDekProtected(recoveredDek)

            // 3. Update PIN
            val pinUpdated = pinManager.setPin(newPin)
            if (!pinUpdated) {
                return Result.failure(IllegalStateException("Gagal memperbarui PIN"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun changePin(currentPin: String, newPin: String): Result<Unit> {
        return try {
            if (currentPin.length != 6 || !currentPin.all { it.isDigit() }) {
                return Result.failure(IllegalArgumentException("PIN saat ini harus berupa 6 digit angka"))
            }
            if (newPin.length != 6 || !newPin.all { it.isDigit() }) {
                return Result.failure(IllegalArgumentException("PIN baru harus berupa 6 digit angka"))
            }
            if (!verifyPin(currentPin)) {
                return Result.failure(IllegalArgumentException("PIN saat ini salah"))
            }
            val pinUpdated = pinManager.setPin(newPin)
            if (!pinUpdated) {
                return Result.failure(IllegalStateException("Gagal memperbarui PIN"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun clearSecurity(): Result<Unit> {
        return try {
            pinManager.clearPin()
            recoveryManager.clearRecovery()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
