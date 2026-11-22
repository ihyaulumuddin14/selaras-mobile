package com.example.selaras.core.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

data class AuthSession(
    val token: String? = null,
    val email: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val lastSyncedAt: Instant? = null
) {
    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank()
}

/**
 * Manages user authentication credentials and synchronization state using private SharedPreferences.
 */
class AuthSessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow(loadSession())
    val sessionState: StateFlow<AuthSession> = _sessionState.asStateFlow()

    fun getSession(): AuthSession = _sessionState.value

    fun getToken(): String? = _sessionState.value.token

    fun getUserEmail(): String? = _sessionState.value.email

    fun getUserId(): String? = _sessionState.value.userId

    fun getUserName(): String? = _sessionState.value.userName

    fun getLastSyncedAt(): Instant? = _sessionState.value.lastSyncedAt

    fun isLoggedIn(): Boolean = _sessionState.value.isLoggedIn

    @Synchronized
    fun saveSession(
        token: String,
        email: String,
        userId: String,
        userName: String?
    ) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .apply()

        _sessionState.value = _sessionState.value.copy(
            token = token,
            email = email,
            userId = userId,
            userName = userName
        )
    }

    @Synchronized
    fun updateLastSyncedAt(timestamp: Instant) {
        prefs.edit()
            .putString(KEY_LAST_SYNCED_AT, timestamp.toString())
            .apply()

        _sessionState.value = _sessionState.value.copy(lastSyncedAt = timestamp)
    }

    @Synchronized
    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EMAIL)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_LAST_SYNCED_AT)
            .apply()

        _sessionState.value = AuthSession()
    }

    private fun loadSession(): AuthSession {
        val token = prefs.getString(KEY_TOKEN, null)
        val email = prefs.getString(KEY_EMAIL, null)
        val userId = prefs.getString(KEY_USER_ID, null)
        val userName = prefs.getString(KEY_USER_NAME, null)
        val lastSyncedStr = prefs.getString(KEY_LAST_SYNCED_AT, null)
        val lastSyncedAt = lastSyncedStr?.let {
            runCatching { Instant.parse(it) }.getOrNull()
        }

        return AuthSession(
            token = token,
            email = email,
            userId = userId,
            userName = userName,
            lastSyncedAt = lastSyncedAt
        )
    }

    companion object {
        private const val PREFS_NAME = "selaras_auth_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USER_NAME = "auth_user_name"
        private const val KEY_LAST_SYNCED_AT = "auth_last_synced_at"
    }
}
