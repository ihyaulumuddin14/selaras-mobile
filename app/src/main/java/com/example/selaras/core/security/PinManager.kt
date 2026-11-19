package com.example.selaras.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Manages user PIN for App Lock and biometric UI fallback.
 *
 * IMPORTANT: User PIN is NEVER used as the Database Encryption Key (DEK).
 * PIN is strictly for application access control to prevent locking out the user
 * from the database if they change or reset their PIN.
 */
class PinManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    fun isPinSet(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)
    }

    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val hash = hashPin(pin.toCharArray(), salt)

        prefs.edit()
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHashBase64 = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSaltBase64 = prefs.getString(KEY_PIN_SALT, null) ?: return false

        val storedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP)
        val storedSalt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)

        val computedHash = hashPin(pin.toCharArray(), storedSalt)
        return MessageDigest.isEqual(storedHash, computedHash)
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .apply()
    }

    private fun hashPin(pinChars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pinChars, salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    companion object {
        private const val PREFS_NAME = "selaras_pin_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val SALT_LENGTH_BYTES = 16
        private const val ITERATION_COUNT = 10_000
        private const val KEY_LENGTH_BITS = 256
    }
}
