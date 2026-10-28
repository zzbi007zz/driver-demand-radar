package com.february10.driverradar.repo

import com.february10.driverradar.data.local.LocationDao
import com.february10.driverradar.data.local.LocationEntity
import com.february10.driverradar.data.local.LocationSource
import kotlinx.coroutines.flow.Flow

/**
 * Write path for saved locations. [upsert] dedupes on [LocationEntity.placeId]:
 * a second save of the same Google place updates the existing row in place
 * (keeping its id + createdAt) instead of inserting a duplicate.
 */
class LocationRepository(private val dao: LocationDao) {

    suspend fun upsert(entity: LocationEntity): Long {
        if (entity.placeId != null) {
            val existing = dao.findByPlaceId(entity.placeId)
            if (existing != null) {
                dao.update(existing.copy(
                    source = entity.source,
                    name = entity.name,
                    category = entity.category,
                    lat = entity.lat,
                    lng = entity.lng,
                ))
                return existing.id
            }
        }
        return dao.insert(entity)
    }

    fun newest(limit: Int): Flow<List<LocationEntity>> = dao.newest(limit)

    fun recentLocations(limit: Int): Flow<List<LocationEntity>> = dao.recentLocations(limit)

    /** Convenience for Phase 02 call sites building a location from a place hit. */
    fun toEntity(place: com.february10.driverradar.data.places.SearchablePlace): LocationEntity {
        val source = if (place.placeId != null) LocationSource.GOOGLE_PLACE else LocationSource.MANUAL
        return LocationEntity(
            source = source.value,
            placeId = place.placeId,
            name = place.name,
            category = place.category ?: "unknown",
            lat = place.lat,
            lng = place.lng,
            createdAt = System.currentTimeMillis(),
        )
    }
}
