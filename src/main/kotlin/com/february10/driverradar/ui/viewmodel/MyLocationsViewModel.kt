package com.february10.driverradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.february10.driverradar.data.local.LocationDao
import com.february10.driverradar.data.local.LocationEntity
import com.february10.driverradar.data.local.LocationSource
import com.february10.driverradar.repo.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A saved location plus how much pickup activity it has. */
data class MyLocationRow(
    val location: LocationEntity,
    val pickupCount: Int,
    val lastPickupTime: Long?,
)

/**
 * State for the "My Locations" tab: saved locations ordered last-pickup-first
 * with per-location pickup counts joined in one Room pass
 * ([LocationRepository.recentWithStats]), plus a manual "add location" write
 * path (KISS: name + optional coordinates, no geocoding).
 */
class MyLocationsViewModel(
    private val repository: LocationRepository,
    private val recentLimit: Int = 50,
) : ViewModel() {

    private val rowsFlow: Flow<List<MyLocationRow>> =
        repository.recentWithStats(recentLimit)
            .map { stats -> stats.map { toRow(it) } }

    /** Hot, replay-1 stream: safe to (re)collect from Compose without extra scope. */
    val rows: StateFlow<List<MyLocationRow>> = rowsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList(),
    )

    /**
     * Manual location entry: no placeId, so it can't dedupe against a Google
     * place later — acceptable for the KISS v1 (documented in the plan).
     */
    fun addManualLocation(name: String, lat: Double? = null, lng: Double? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.upsert(
                LocationEntity(
                    source = LocationSource.MANUAL.value,
                    placeId = null,
                    name = trimmed,
                    category = "manual",
                    lat = lat ?: 0.0,
                    lng = lng ?: 0.0,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun toRow(s: LocationDao.LocationWithStats): MyLocationRow =
        MyLocationRow(
            location = LocationEntity(
                id = s.id,
                source = s.source,
                placeId = s.placeId,
                name = s.name,
                category = s.category,
                lat = s.lat,
                lng = s.lng,
                createdAt = s.createdAt,
            ),
            pickupCount = s.pickupCount,
            lastPickupTime = s.lastPickupTime,
        )
}
