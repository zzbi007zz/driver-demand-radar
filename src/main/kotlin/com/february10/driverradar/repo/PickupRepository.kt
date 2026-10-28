package com.february10.driverradar.repo

import com.february10.driverradar.data.local.PickupDao
import com.february10.driverradar.data.local.PickupEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Log of individual pickups. Local-only: no PII fields, aliases and notes
 * are free text chosen by the caller.
 */
class PickupRepository(private val dao: PickupDao) {

    /**
     * Records a new pickup. [loggedAt] bookkeeping is set here (implicit
     * "now") so callers never have to thread it; [actualPickupTime] is the
     * caller's value (default now, backdatable from the log sheet).
     */
    suspend fun insert(
        locationId: Long,
        customerAlias: String,
        note: String?,
        actualPickupTime: Long = System.currentTimeMillis(),
    ): Long = dao.insert(
        PickupEntity(
            locationId = locationId,
            customerAlias = customerAlias.trim(),
            note = note?.trim()?.takeIf(String::isNotEmpty),
            loggedAt = System.currentTimeMillis(),
            actualPickupTime = actualPickupTime,
        )
    )

    /** KISS alias of [insert] for existing call sites. */
    suspend fun log(
        locationId: Long,
        customerAlias: String,
        note: String?,
        actualPickupTime: Long = System.currentTimeMillis(),
    ): Long = insert(locationId, customerAlias, note, actualPickupTime)

    /**
     * Edit an existing row. Callers hold the full row (from the history list)
     * and rebuild it; [PickupEntity.id] and [PickupEntity.loggedAt] are
     * preserved untouched — re-stamping loggedAt on an edit would mask the
     * "logged later" badge.
     */
    suspend fun update(pickup: PickupEntity) {
        dao.update(
            pickup.copy(
                customerAlias = pickup.customerAlias.trim(),
                note = pickup.note?.trim()?.takeIf(String::isNotEmpty),
            )
        )
    }

    /** Plain delete (local-only scope — no tombstones, no sync state). */
    suspend fun delete(id: Long) = dao.delete(id)

    /** Newest actual pickup first; drives the per-location history list. */
    fun historyByLocation(locationId: Long): Flow<List<PickupEntity>> =
        dao.historyByLocation(locationId)

    /** Most-recently-used distinct aliases, for the log-sheet autocomplete. */
    fun recentAliases(): Flow<List<String>> = dao.distinctAliases()

    /** One row by id (for edit-prefill); null when already deleted. */
    suspend fun byId(id: Long): PickupEntity? = dao.byId(id).first()

    /** Undo path: re-insert a deleted row with its original id/times. No-op if the row reappeared. */
    suspend fun restore(pickup: PickupEntity) = dao.insertWithId(pickup)
}
