package com.february10.driverradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.february10.driverradar.data.local.LocationEntity
import com.february10.driverradar.repo.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * State for the "My Locations" tab. Streams saved locations via
 * [LocationRepository.recentLocations] (last-pickup-first ordering) into a
 * [StateFlow] the screen collects. No write path yet — that ships with Phase
 * 02/03, so this VM only observes.
 */
class MyLocationsViewModel(
    repository: LocationRepository,
    private val recentLimit: Int = 50,
) : ViewModel() {

    private val locationsFlow: Flow<List<LocationEntity>> = repository.recentLocations(recentLimit)

    /** Hot, replay-1 stream: safe to (re)collect from Compose without extra scope. */
    val locations: StateFlow<List<LocationEntity>> = locationsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList(),
    )
}
