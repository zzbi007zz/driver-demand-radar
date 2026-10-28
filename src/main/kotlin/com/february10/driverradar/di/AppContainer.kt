package com.february10.driverradar.di

import android.content.Context
import com.february10.driverradar.data.local.DemandRadarDb
import com.february10.driverradar.data.places.NoopPlaceSearcher
import com.february10.driverradar.data.places.PlaceSearcher
import com.february10.driverradar.repo.LocationRepository
import com.february10.driverradar.repo.PickupRepository

/**
 * Hand-rolled DI root (no Hilt). Created once in [DriverDemandRadarApplication].
 * Phase 02 swaps [NoopPlaceSearcher] for a Places-backed impl here, without
 * touching call sites.
 */
class AppContainer(
    val db: DemandRadarDb,
    val locationRepository: LocationRepository,
    val pickupRepository: PickupRepository,
    val placeSearcher: PlaceSearcher,
) {
    companion object {
        fun create(context: Context): AppContainer {
            val db = DemandRadarDb.get(context)
            return AppContainer(
                db = db,
                locationRepository = LocationRepository(db.locationDao()),
                pickupRepository = PickupRepository(db.pickupDao()),
                placeSearcher = NoopPlaceSearcher(),
            )
        }
    }
}
