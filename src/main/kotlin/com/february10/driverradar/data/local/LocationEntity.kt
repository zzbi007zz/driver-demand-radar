package com.february10.driverradar.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Where a saved location came from. `source` is persisted as a plain string so
 * the DB schema stays trivial; Phase 02 maps Google Place results onto these.
 */
enum class LocationSource(val value: String) {
    /** Manually pinned on the map (no Google place id). */
    MANUAL("manual"),

    /** Imported from a Google Places result (carries a stable placeId). */
    GOOGLE_PLACE("google_place"),
}

/**
 * A saved pickup location.
 *
 * Dedupe invariant: a location is "the same place" when its [placeId] matches.
 * The unique index is effectively a *partial* constraint — SQLite treats NULLs
 * as distinct from one another, so rows with different (or null) placeId values
 * coexist, while two rows sharing the same non-null placeId collide. The
 * repository layer (LocationRepository.upsert) turns that collision into an
 * in-place update instead of a duplicate row.
 */
@Entity(
    tableName = "locations",
    indices = [Index(value = ["placeId"], unique = true, name = "idx_locations_place_id")],
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Where the location came from (see [LocationSource]). */
    val source: String,
    /** Stable Google place id, or null for manual map taps. */
    val placeId: String?,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    /** Epoch millis. */
    val createdAt: Long,
)
