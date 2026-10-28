package com.february10.driverradar.model

import java.util.Calendar

/**
 * One cell of the 7x24 demand-heat grid: pickups on [weekday] during [hour]
 * (local time), counted by `actualPickupTime`, so backdated entries land in
 * the slot the ride actually happened.
 *
 * Weekday 0 = Monday ... 6 = Sunday (mirrors [java.time.DayOfWeek]).
 */
data class HeatBucket(
    val weekday: Int,
    val hour: Int,
    val count: Int,
) {
    companion object {
        /** Same (weekday 0=Mon, hour) the Room bucketing query uses — Calendar, local time. */
        fun of(epochMillis: Long): HeatBucket {
            val c = Calendar.getInstance().apply { timeInMillis = epochMillis }
            val weekday = (c.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
            return HeatBucket(weekday, c.get(Calendar.HOUR_OF_DAY), 0)
        }
    }
}

/**
 * All 168 [HeatBucket]s for one location's trailing-8-week pickups. The
 * bucket list carries only non-zero cells; the grid fills missing cells
 * with 0 on the client.
 */
data class HeatMatrix(
    val locationId: Long,
    val buckets: List<HeatBucket>,
    /** Pickups in the 8-week window — the gate denominator. */
    val totalCount: Int,
) {
    /** Busiest slots first; used for the "top 3" text rows. Ties keep hour order. */
    fun topSlots(limit: Int = 3): List<HeatBucket> =
        buckets.sortedWith(compareByDescending<HeatBucket> { it.count }.thenBy { it.weekday }.thenBy { it.hour })
            .take(limit)
}
