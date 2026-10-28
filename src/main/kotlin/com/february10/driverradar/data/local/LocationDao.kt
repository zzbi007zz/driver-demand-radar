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
}
