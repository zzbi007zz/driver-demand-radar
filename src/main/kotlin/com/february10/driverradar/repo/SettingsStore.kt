package com.february10.driverradar.repo

import android.content.Context
import android.content.SharedPreferences

/**
 * User settings, plain SharedPreferences (DataStore is overkill for two
 * keys). [minHeatCount] is the heat-strip gate: below it the detail screen
 * shows the "N of gate" progress state instead of a heat grid.
 */
class SettingsStore(context: Context) {

    private companion object {
        const val PREFS = "dr-settings"
        const val KEY_MIN_HEAT_COUNT = "min_heat_count"
        const val KEY_LAST_EXPORT_AT = "last_export_at"
        const val DEFAULT_MIN_HEAT_COUNT = 15
        const val MIN = 5
        const val MAX = 50
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Heat gate (default 15). Clamped to [MIN]..[MAX] on write. */
    var minHeatCount: Int
        get() = prefs.getInt(KEY_MIN_HEAT_COUNT, DEFAULT_MIN_HEAT_COUNT)
        set(value) = prefs.edit()
            .putInt(KEY_MIN_HEAT_COUNT, value.coerceIn(MIN, MAX))
            .apply()

    /** UTC epoch millis of the last successful JSON export, null = never. */
    var lastExportAt: Long?
        get() = prefs.contains(KEY_LAST_EXPORT_AT).let { stored ->
            if (stored) prefs.getLong(KEY_LAST_EXPORT_AT, 0L) else null
        }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_LAST_EXPORT_AT)
                else putLong(KEY_LAST_EXPORT_AT, value)
            }.apply()
        }
}
