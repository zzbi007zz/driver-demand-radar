package com.february10.driverradar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PickupDao {

    @Insert
    suspend fun insert(pickup: PickupEntity): Long

    /** Pickup history for one location, newest actual pickup first. */
    @Query("SELECT * FROM pickups WHERE locationId = :locationId ORDER BY actualPickupTime DESC")
    fun historyByLocation(locationId: Long): Flow<List<PickupEntity>>
}
