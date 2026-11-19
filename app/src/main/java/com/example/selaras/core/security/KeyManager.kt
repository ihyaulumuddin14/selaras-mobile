package com.example.selaras.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the Database Encryption Key (DEK) with Android Keystore protection.
 *
 * Architecture:
 * 1. DEK is a cryptographically strong 256-bit random byte array generated via SecureRandom.
 * 2. Key Encryption Key (KEK) is stored in the hardware-backed Android Keystore (AES-256-GCM).
 * 3. DEK is encrypted using KEK before being persisted to private application storage.
 * 4. User PIN is NEVER used as the DEK.
 */
class KeyManager(
    private val context: Context,
    private val keyStoreProvider: String = ANDROID_KEYSTORE_PROVIDER
) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    /**
     * Retrieves the existing DEK or generates a new one protected by the Keystore.
     */
    @Synchronized
    fun getOrCreateDatabaseKey(): ByteArray {
        val encryptedDekBase64 = prefs.getString(KEY_ENCRYPTED_DEK, null)
        val ivBase64 = prefs.getString(KEY_IV, null)

        return if (encryptedDekBase64 != null && ivBase64 != null) {
            val encryptedDek = Base64.decode(encryptedDekBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            decryptDekWithKek(encryptedDek, iv)
        } else {
            val newDek = ByteArray(DEK_SIZE_BYTES).also { secureRandom.nextBytes(it) }
            saveDekProtected(newDek)
            newDek
        }
    }

    /**
     * Stores or updates the DEK protected by Keystore KEK.
     */
    @Synchronized
    fun saveDekProtected(dek: ByteArray) {
        val kek = getOrCreateKek()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, kek)
        val iv = cipher.iv
        val encryptedDek = cipher.doFinal(dek)

        prefs.edit()
            .putString(KEY_ENCRYPTED_DEK, Base64.encodeToString(encryptedDek, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    /**
     * Checks whether an encrypted DEK already exists in storage.
     */
    fun hasDatabaseKey(): Boolean {
        return prefs.contains(KEY_ENCRYPTED_DEK) && prefs.contains(KEY_IV)
    }

    private fun decryptDekWithKek(encryptedDek: ByteArray, iv: ByteArray): ByteArray {
        val kek = getOrCreateKek()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, kek, spec)
        return cipher.doFinal(encryptedDek)
    }

    private fun getOrCreateKek(): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreProvider).apply { load(null) }

        if (keyStore.containsAlias(KEK_ALIAS)) {
            val entry = keyStore.getEntry(KEK_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            keyStoreProvider
        )

        val spec = KeyGenParameterSpec.Builder(
            KEK_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val PREFS_NAME = "selaras_key_prefs"
        private const val KEY_ENCRYPTED_DEK = "encrypted_dek"
        private const val KEY_IV = "dek_iv"
        private const val KEK_ALIAS = "selaras_database_kek"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        const val DEK_SIZE_BYTES = 32 // 256-bit key
    }
}
