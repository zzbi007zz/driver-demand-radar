package com.february10.driverradar.repo

import com.february10.driverradar.data.local.PickupDao
import com.february10.driverradar.model.HeatBucket
import com.february10.driverradar.model.HeatMatrix
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 7x24 demand heat for one location, computed entirely in Room (two SQL
 * queries — buckets + window total — combined into a [HeatMatrix] stream).
 * Buckets key off `actualPickupTime`, so backdated entries count where the
 * ride happened. The min-count gate is applied client-side by the UI from
 * [SettingsStore.minHeatCount].
 */
class HeatRepository(pickupDao: PickupDao) {

    private val dao = pickupDao

    companion object {
        /** Trailing window, confirmed in the plan's validation. */
        const val WINDOW_WEEKS = 8

        private const val MILLIS_PER_WEEK = 7L * 24L * 60L * 60L * 1000L

        /** Rolling 8-week start, pinned to local-time midnight. Recomputed per snapshot. */
        fun windowStartMillis(now: Long = System.currentTimeMillis()): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = now - (WINDOW_WEEKS * MILLIS_PER_WEEK)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
    }

    /**
     * Reactive (buckets, total) for [locationId]. The start instant is baked
     * in at call time; it re-evaluates only when pickup rows change.
     */
    fun matrixFor(locationId: Long): Flow<HeatMatrix> {
        val start = windowStartMillis()
        return combine(
            dao.heatBuckets(locationId, start),
            dao.heatTotal(locationId, start),
        ) { rows, total ->
            HeatMatrix(
                locationId = locationId,
                buckets = rows.map { HeatBucket(it.weekday, it.hour, it.count) },
                totalCount = total,
            )
        }
    }

}
