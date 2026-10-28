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
