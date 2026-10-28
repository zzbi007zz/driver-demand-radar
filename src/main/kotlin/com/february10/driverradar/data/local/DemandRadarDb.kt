package com.february10.driverradar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LocationEntity::class, PickupEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class DemandRadarDb : RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun pickupDao(): PickupDao

    companion object {
        private const val NAME = "demand-radar.db"

        @Volatile
        private var instance: DemandRadarDb? = null

        /** Process-wide singleton; Room handles its own thread safety. */
        fun get(context: Context): DemandRadarDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DemandRadarDb::class.java,
                    NAME,
                ).build().also { instance = it }
            }

        /** In-memory DB for JVM unit tests (see src/test). */
        fun inMemory(context: Context): DemandRadarDb =
            Room.inMemoryDatabaseBuilder(context, DemandRadarDb::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
