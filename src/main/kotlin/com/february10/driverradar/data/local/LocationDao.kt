package com.february10.driverradar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    /** REPLACE so upserts that carry an explicit [LocationEntity.id] update in place. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: LocationEntity): Long

    @Update
    suspend fun update(location: LocationEntity)

    @Query("SELECT * FROM locations WHERE placeId = :placeId LIMIT 1")
    suspend fun findByPlaceId(placeId: String): LocationEntity?

    @Query("SELECT * FROM locations ORDER BY createdAt DESC LIMIT :limit")
    fun newest(limit: Int): Flow<List<LocationEntity>>

    /**
     * "Recent" = last-pickup-first: locations with the highest actualPickupTime
     * rank first; locations that never had a pickup fall back to createdAt.
     */
    @Query(
        """
        SELECT l.*
        FROM locations l
        LEFT JOIN (
            SELECT locationId, MAX(actualPickupTime) AS lastPickup
            FROM pickups GROUP BY locationId
        ) p ON p.locationId = l.id
        ORDER BY COALESCE(p.lastPickup, l.createdAt) DESC
        LIMIT :limit
        """
    )
    fun recentLocations(limit: Int): Flow<List<LocationEntity>>

    /**
     * [recentLocations] plus per-location pickup count + most-recent pickup
     * time, computed in one SQL pass (no N+1). [lastPickupTime] is null when
     * the location has no pickups yet.
     */
    @Query(
        """
        SELECT l.*,
               (SELECT COUNT(*) FROM pickups p1 WHERE p1.locationId = l.id) AS pickupCount,
               (SELECT MAX(p2.actualPickupTime) FROM pickups p2 WHERE p2.locationId = l.id) AS lastPickupTime
        FROM locations l
        ORDER BY COALESCE(
            (SELECT MAX(p3.actualPickupTime) FROM pickups p3 WHERE p3.locationId = l.id),
            l.createdAt
        ) DESC
        LIMIT :limit
        """
    )
    fun recentWithPickupStats(limit: Int): Flow<List<LocationWithStats>>

    /** All rows (for JSON export). */
    @Query("SELECT * FROM locations")
    suspend fun all(): List<LocationEntity>

    /**
     * A location row joined with its pickup aggregates in one query (no N+1).
     * Flat projection — Room can't embed an entity object from SQL columns,
     * so the columns mirror [LocationEntity] field-for-field and the
     * repository folds them into an entity when needed.
     */
    data class LocationWithStats(
        val id: Long,
        val source: String,
        val placeId: String?,
        val name: String,
        val category: String,
        val lat: Double,
        val lng: Double,
        val createdAt: Long,
        val pickupCount: Int,
        val lastPickupTime: Long?,
    )
}
