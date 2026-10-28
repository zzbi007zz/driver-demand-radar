package com.february10.driverradar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PickupDao {

    @Insert
    suspend fun insert(pickup: PickupEntity): Long

    /**
     * 7x24 heat buckets: count pickups per (weekday, hour) for one location.
     *
     * Buckets by [PickupEntity.actualPickupTime] — NOT loggedAt — so a pickup
     * logged later but set "earlier" lands in the slot the ride happened.
     * [windowStartMillis] limits the window (HeatRepository passes
     * now-minus-8-weeks). `strftime` with `unixepoch` expects seconds, so the
     * stored epoch-millis column is divided by 1000 first. `%w` yields
     * 0=Sunday..6=Saturday; the CASE remaps to 0=Monday..6=Sunday to
     * match [com.february10.driverradar.model.HeatBucket] (0=Monday).
     * Grouping in SQL keeps this a single pass — no in-memory loop over all
     * rows. Reactive: the strip refreshes as new pickups are logged.
     */
    @Query(
        """
        SELECT CASE WHEN CAST(strftime('%w', actualPickupTime / 1000, 'unixepoch', 'localtime') AS INTEGER) = 0
                     THEN 6
                     ELSE CAST(strftime('%w', actualPickupTime / 1000, 'unixepoch', 'localtime') AS INTEGER) - 1
                END AS weekday,
               CAST(strftime('%H', actualPickupTime / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hour,
               COUNT(*) AS count
        FROM pickups
        WHERE locationId = :locationId AND actualPickupTime >= :windowStartMillis
        GROUP BY weekday, hour
        """
    )
    fun heatBuckets(locationId: Long, windowStartMillis: Long): Flow<List<HeatBucketRow>>

    /** Pickups in the 8-week window — matches [heatBuckets]'s WHERE clause exactly. */
    @Query("SELECT COUNT(*) FROM pickups WHERE locationId = :locationId AND actualPickupTime >= :windowStartMillis")
    fun heatTotal(locationId: Long, windowStartMillis: Long): Flow<Int>

    /** All rows (for JSON export). */
    @Query("SELECT * FROM pickups")
    suspend fun all(): List<PickupEntity>

    data class HeatBucketRow(
        val weekday: Int,
        val hour: Int,
        val count: Int,
    )

    /** Re-insert with an explicit id (undo-delete restore); no-op if the row reappeared. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWithId(pickup: PickupEntity)

    /** Pickup history for one location, newest actual pickup first. */
    @Query("SELECT * FROM pickups WHERE locationId = :locationId ORDER BY actualPickupTime DESC")
    fun historyByLocation(locationId: Long): Flow<List<PickupEntity>>

    /** A single row by id; powers "edit" prefill in the log sheet. */
    @Query("SELECT * FROM pickups WHERE id = :id LIMIT 1")
    fun byId(id: Long): Flow<PickupEntity?>

    /** Distinct aliases (most-recent usage first), for autocomplete suggestions. */
    @Query(
        """
        SELECT DISTINCT customerAlias AS alias FROM pickups
        WHERE TRIM(customerAlias) <> ''
        ORDER BY (SELECT MAX(p2.loggedAt) FROM pickups p2
                  WHERE p2.customerAlias = pickups.customerAlias) DESC, alias
        """
    )
    fun distinctAliases(): Flow<List<String>>

    @Update
    suspend fun update(pickup: PickupEntity)

    @Query("DELETE FROM pickups WHERE id = :id")
    suspend fun delete(id: Long)
}
