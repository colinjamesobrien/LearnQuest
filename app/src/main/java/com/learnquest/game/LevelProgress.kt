package com.learnquest.game

import android.content.Context

/**
 * Persists level unlock progress in SharedPreferences.
 *
 * Level 1 is always unlocked.  Completing level N automatically unlocks level N+1
 * (up to [TOTAL_LEVELS]).  Progress survives app restarts.
 */
object LevelProgress {

    private const val PREFS_NAME      = "learnquest_progress"
    private const val KEY_MAX_UNLOCKED = "max_unlocked_level"

    const val TOTAL_LEVELS = 5

    /** Returns the highest level the player has unlocked (minimum 1). */
    fun getMaxUnlocked(context: Context): Int =
        prefs(context).getInt(KEY_MAX_UNLOCKED, 1)

    /** Unlocks [level] if it is higher than the current maximum. */
    fun completeLevel(context: Context, level: Int) {
        val next = (level + 1).coerceAtMost(TOTAL_LEVELS)
        val current = getMaxUnlocked(context)
        if (next > current) {
            prefs(context).edit().putInt(KEY_MAX_UNLOCKED, next).apply()
        }
    }

    /** True when [level] ≤ the current unlocked maximum. */
    fun isUnlocked(context: Context, level: Int): Boolean =
        level <= getMaxUnlocked(context)

    /** Resets all progress (useful for testing). */
    fun reset(context: Context) {
        prefs(context).edit().putInt(KEY_MAX_UNLOCKED, 1).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
