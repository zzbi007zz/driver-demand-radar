package com.february10.driverradar.di

import android.content.Context
import com.february10.driverradar.R
import com.february10.driverradar.data.local.DemandRadarDb
import com.february10.driverradar.data.places.GooglePlaceSearcher
import com.february10.driverradar.data.places.PlaceSearcher
import com.february10.driverradar.repo.ExportRepository
import com.february10.driverradar.repo.HeatRepository
import com.february10.driverradar.repo.LocationRepository
import com.february10.driverradar.repo.PickupRepository
import com.february10.driverradar.repo.SettingsStore
import com.february10.driverradar.util.GpsProvider

/**
 * Hand-rolled DI root (no Hilt). Created once in
 * [com.february10.driverradar.DriverDemandRadarApplication].
 *
 * The [PlaceSearcher] is a [GooglePlaceSearcher] backed by the resValue-injected
 * Maps key; when that key is empty the searcher simply reports NoKey so the UI
 * can gate. The [NoopPlaceSearcher] seam from Phase 01 is no longer needed.
 */
class AppContainer(
    val db: DemandRadarDb,
    val locationRepository: LocationRepository,
    val pickupRepository: PickupRepository,
    val heatRepository: HeatRepository,
    val exportRepository: ExportRepository,
    val settingsStore: SettingsStore,
    val placeSearcher: PlaceSearcher,
    val gpsProvider: GpsProvider,
) {
    companion object {
        fun create(context: Context): AppContainer {
            val db = DemandRadarDb.get(context)
            val appContext = context.applicationContext
            val mapsKey = appContext.getString(R.string.GOOGLE_MAPS_API_KEY) ?: ""
            return AppContainer(
                db = db,
                locationRepository = LocationRepository(db.locationDao()),
                pickupRepository = PickupRepository(db.pickupDao()),
                heatRepository = HeatRepository(db.pickupDao()),
                exportRepository = ExportRepository(appContext, db.locationDao(), db.pickupDao()),
                settingsStore = SettingsStore(appContext),
                placeSearcher = GooglePlaceSearcher(mapsKey),
                gpsProvider = GpsProvider(appContext),
            )
        }
    }
}
