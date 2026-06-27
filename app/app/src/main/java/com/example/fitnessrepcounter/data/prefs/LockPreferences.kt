package com.example.fitnessrepcounter.data.prefs

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class LockPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_COMPLETED_TIME = "last_completed_time"
    }

    fun markCompleted() {
        prefs.edit().putLong(KEY_LAST_COMPLETED_TIME, System.currentTimeMillis()).apply()
    }

    /**
     * Checks if the user is locked today.
     * The lock resets at 3 AM every day.
     */
    fun isLockedToday(): Boolean {
        val lastCompleted = prefs.getLong(KEY_LAST_COMPLETED_TIME, 0L)
        if (lastCompleted == 0L) return true

        val now = Calendar.getInstance()
        
        // Define the "start" of the current logical day as 3:00 AM today
        val startOfLogicalDay = Calendar.getInstance().apply {
            if (now.get(Calendar.HOUR_OF_DAY) < 3) {
                // If it's currently before 3 AM, the logical day started at 3 AM yesterday
                add(Calendar.DAY_OF_YEAR, -1)
            }
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If last completed time is before the start of the current logical day, they are locked again
        return lastCompleted < startOfLogicalDay.timeInMillis
    }
}
