package com.example.selaras.domain.repository

/**
 * Contract for application security, PIN authentication, and recovery operations (ADR-002, ADR-003).
 */
interface SecurityRepository {

    /**
     * Checks whether an App Lock PIN has been established.
     */
    fun isPinSet(): Boolean

    /**
     * Checks whether emergency recovery has been configured.
     */
    fun isRecoveryConfigured(): Boolean

    /**
     * Sets up a new 6-digit PIN and generates an emergency recovery code.
     * Backs up the Database Encryption Key (DEK) wrapped with the recovery key.
     *
     * @param pin The 6-digit numeric PIN.
     * @return Result containing the formatted 16-character recovery code (e.g. "ABCD-EFGH-JKLM-NPQR").
     */
    fun setupPinAndRecovery(pin: String): Result<String>

    /**
     * Verifies if the entered PIN matches the stored PBKDF2 hash.
     *
     * @param pin The 6-digit numeric PIN to verify.
     * @return True if valid, false otherwise.
     */
    fun verifyPin(pin: String): Boolean

    /**
     * Verifies if the entered recovery code is valid and matches the stored hash.
     *
     * @param code The 16-character recovery code.
     * @return True if valid, false otherwise.
     */
    fun verifyRecoveryCode(code: String): Boolean

    /**
     * Resets the user's PIN using a verified recovery code.
     *
     * @param recoveryCode The 16-character recovery code.
     * @param newPin The new 6-digit numeric PIN.
     * @return Result indicating success or failure.
     */
    fun resetPinWithRecovery(recoveryCode: String, newPin: String): Result<Unit>

    /**
     * Changes the user's PIN to a new 6-digit PIN after verifying the current PIN.
     *
     * @param currentPin The current 6-digit numeric PIN.
     * @param newPin The new 6-digit numeric PIN.
     * @return Result indicating success or failure.
     */
    fun changePin(currentPin: String, newPin: String): Result<Unit>

    /**
     * Clears stored PIN and recovery configuration (used during data reset/testing).
     */
    fun clearSecurity(): Result<Unit>
}
