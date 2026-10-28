package com.february10.driverradar.repo

import com.february10.driverradar.data.local.PickupDao
import com.february10.driverradar.data.local.PickupEntity
import kotlinx.coroutines.flow.Flow

/**
 * Log of individual pickups. Local-only: no PII fields, aliases and notes
 * are free text chosen by the caller.
 */
class PickupRepository(private val dao: PickupDao) {

    /** Records a pickup against an existing [LocationRepository]-managed location. */
    suspend fun log(
        locationId: Long,
        customerAlias: String,
        note: String?,
        actualPickupTime: Long = System.currentTimeMillis(),
        loggedAt: Long = System.currentTimeMillis(),
    ): Long = dao.insert(
        PickupEntity(
            locationId = locationId,
            customerAlias = customerAlias,
            note = note?.trim()?.takeIf(String::isNotEmpty),
            loggedAt = loggedAt,
            actualPickupTime = actualPickupTime,
        )
    )

    /** Newest actual pickup first; drives the per-location history sheet. */
    fun historyByLocation(locationId: Long): Flow<List<PickupEntity>> =
        dao.historyByLocation(locationId)
}
