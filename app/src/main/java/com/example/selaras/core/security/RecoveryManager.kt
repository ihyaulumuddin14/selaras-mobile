package com.example.selaras.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages Recovery Code generation, hashing, and DEK backup wrapping.
 *
 * Security rules:
 * 1. Recovery Code is NEVER stored in plaintext.
 * 2. Stored as salted PBKDF2 hash.
 * 3. A copy of the DEK is encrypted using a key derived from the Recovery Code via PBKDF2 + AES-GCM.
 * 4. Enables recovering the database if Keystore KEK is reset or invalidated.
 */
class RecoveryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    fun isRecoveryConfigured(): Boolean {
        return prefs.contains(KEY_CODE_HASH) &&
                prefs.contains(KEY_CODE_SALT) &&
                prefs.contains(KEY_ENCRYPTED_DEK_BACKUP) &&
                prefs.contains(KEY_BACKUP_IV)
    }

    /**
     * Generates a new Recovery Code, saves its salted hash, and encrypts a backup copy of the DEK.
     * @param currentDek The 256-bit Database Encryption Key to back up.
     * @return Formatted Recovery Code string (e.g. "ABCD-EFGH-JKLM-NPQR") to be shown to the user.
     */
    fun setupRecovery(currentDek: ByteArray): String {
        val code = generateRandomCode()
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

        val normalized = normalizeCode(code)
        val hash = hashRecoveryCode(normalized.toCharArray(), salt)
        val derivedKey = deriveKeyFromCode(normalized.toCharArray(), salt)

        // Encrypt DEK using derived recovery key
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, derivedKey)
        val iv = cipher.iv
        val encryptedBackup = cipher.doFinal(currentDek)

        prefs.edit()
            .putString(KEY_CODE_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_CODE_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_ENCRYPTED_DEK_BACKUP, Base64.encodeToString(encryptedBackup, Base64.NO_WRAP))
            .putString(KEY_BACKUP_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()

        return code
    }

    /**
     * Verifies the recovery code and decrypts the backup DEK.
     * @return The 256-bit DEK if recovery code is valid, or null if invalid.
     */
    fun recoverDatabaseKey(inputCode: String): ByteArray? {
        val storedHashBase64 = prefs.getString(KEY_CODE_HASH, null) ?: return null
        val storedSaltBase64 = prefs.getString(KEY_CODE_SALT, null) ?: return null
        val storedBackupBase64 = prefs.getString(KEY_ENCRYPTED_DEK_BACKUP, null) ?: return null
        val storedIvBase64 = prefs.getString(KEY_BACKUP_IV, null) ?: return null

        val storedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP)
        val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
        val encryptedBackup = Base64.decode(storedBackupBase64, Base64.NO_WRAP)
        val iv = Base64.decode(storedIvBase64, Base64.NO_WRAP)

        val normalized = normalizeCode(inputCode)
        val computedHash = hashRecoveryCode(normalized.toCharArray(), salt)

        if (!MessageDigest.isEqual(storedHash, computedHash)) {
            return null
        }

        val derivedKey = deriveKeyFromCode(normalized.toCharArray(), salt)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, derivedKey, spec)

        return cipher.doFinal(encryptedBackup)
    }

    fun clearRecovery() {
        prefs.edit()
            .remove(KEY_CODE_HASH)
            .remove(KEY_CODE_SALT)
            .remove(KEY_ENCRYPTED_DEK_BACKUP)
            .remove(KEY_BACKUP_IV)
            .apply()
    }

    private fun generateRandomCode(): String {
        // Excludes ambiguous characters (0, O, 1, I, L)
        val chars = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
        val raw = StringBuilder()
        for (i in 0 until 16) {
            raw.append(chars[secureRandom.nextInt(chars.length)])
        }
        return raw.chunked(4).joinToString("-")
    }

    private fun normalizeCode(code: String): String {
        return code.uppercase().replace("-", "").replace(" ", "").trim()
    }

    private fun hashRecoveryCode(codeChars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(codeChars, salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun deriveKeyFromCode(codeChars: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(codeChars, salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    companion object {
        private const val PREFS_NAME = "selaras_recovery_prefs"
        private const val KEY_CODE_HASH = "recovery_code_hash"
        private const val KEY_CODE_SALT = "recovery_code_salt"
        private const val KEY_ENCRYPTED_DEK_BACKUP = "recovery_encrypted_dek"
        private const val KEY_BACKUP_IV = "recovery_backup_iv"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val SALT_LENGTH_BYTES = 16
        private const val ITERATION_COUNT = 10_000
        private const val KEY_LENGTH_BITS = 256
    }
}
