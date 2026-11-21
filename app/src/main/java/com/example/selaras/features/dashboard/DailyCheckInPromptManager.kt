package com.example.selaras.features.dashboard

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/**
 * Tracks daily check-in prompt dialog visibility (shown on the user's first entry of the day if not checked in).
 */
open class DailyCheckInPromptManager(
    private val prefs: SharedPreferences? = null,
    private val dateProvider: () -> LocalDate = { LocalDate.now() }
) {
    private var inMemoryPromptDate: String? = null

    /**
     * Returns true if the daily check-in prompt dialog has already been shown today.
     */
    open fun hasPromptedToday(): Boolean {
        val todayStr = dateProvider().toString()
        val lastDate = prefs?.getString(KEY_LAST_PROMPT_DATE, null) ?: inMemoryPromptDate
        return lastDate == todayStr
    }

    /**
     * Marks that the daily check-in prompt dialog has been shown or interacted with today.
     */
    open fun markPromptedToday() {
        val todayStr = dateProvider().toString()
        prefs?.edit()?.putString(KEY_LAST_PROMPT_DATE, todayStr)?.apply()
        inMemoryPromptDate = todayStr
    }

    /**
     * Clears stored prompt state (used during data reset or testing).
     */
    open fun clear() {
        prefs?.edit()?.remove(KEY_LAST_PROMPT_DATE)?.apply()
        inMemoryPromptDate = null
    }

    companion object {
        private const val PREFS_NAME = "selaras_daily_prompt_prefs"
        private const val KEY_LAST_PROMPT_DATE = "last_checkin_prompt_date"

        fun create(context: Context): DailyCheckInPromptManager {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return DailyCheckInPromptManager(prefs)
        }
    }
}
