package com.february10.driverradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.february10.driverradar.data.local.LocationEntity
import com.february10.driverradar.data.local.PickupEntity
import com.february10.driverradar.data.places.SearchablePlace
import com.february10.driverradar.repo.LocationRepository
import com.february10.driverradar.repo.PickupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * State + actions for the Log Pickup sheet. One save path: location
 * (prefilled, recent, or a new manual name), alias (autocomplete + free
 * text), optional note, backdatable time.
 *
 * Activity-scoped: [start] resets the sheet for a new session (FAB → no
 * prefill; "log here" from a location detail → prefilled, picker skipped).
 */
class LogPickupViewModel(
    private val locationRepository: LocationRepository,
    private val pickupRepository: PickupRepository,
) : ViewModel() {

    data class SheetUiState(
        val recents: List<LocationEntity> = emptyList(),
        val selectedLocation: LocationEntity? = null,
        val newLocationName: String = "",
        val aliasText: String = "",
        val note: String = "",
        val timeMillis: Long = System.currentTimeMillis(),
        val justSaved: Boolean = false,
    )

    private val _state = MutableStateFlow(SheetUiState())
    val state: StateFlow<SheetUiState> = _state

    /** Distinct aliases, most-recent-first, for autocomplete. */
    val aliases: StateFlow<List<String>> = pickupRepository.recentAliases()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            // Plain location list (last-pickup-first); no aggregates needed here.
            locationRepository.recentLocations(10)
                .collect { recents -> _state.update { it.copy(recents = recents) } }
        }
    }

    /** Reset the sheet for a new logging session; [prefill] skips the picker. */
    fun start(prefill: LocationEntity? = null) {
        _state.value = SheetUiState(selectedLocation = prefill)
    }

    fun onLocationSelected(location: LocationEntity) =
        _state.update { it.copy(selectedLocation = location, newLocationName = "") }

    fun onNewLocationName(name: String) = _state.update { it.copy(newLocationName = name, selectedLocation = null) }
    fun onAliasTextChange(text: String) = _state.update { it.copy(aliasText = text) }
    fun onNoteChange(note: String) = _state.update { it.copy(note = note) }
    fun onTimeChips(millis: Long) = _state.update { it.copy(timeMillis = millis) }
    fun onTimePickerConfirmed(dateMillis: Long, timeOfDayMillis: Long) =
        _state.update { it.copy(timeMillis = dateMillis + timeOfDayMillis) }

    /** One save. Returns false when nothing was written (empty alias or no location). */
    suspend fun save(): Boolean {
        val s = _state.value
        val alias = s.aliasText.trim()
        if (alias.isEmpty()) return false

        val locationId = if (s.newLocationName.isNotBlank()) {
            locationRepository.upsert(
                locationRepository.toEntity(
                    SearchablePlace(name = s.newLocationName.trim(), lat = 0.0, lng = 0.0, placeId = null, category = "manual")
                )
            )
        } else {
            s.selectedLocation?.id ?: return false
        }

        pickupRepository.insert(
            locationId = locationId,
            customerAlias = alias,
            note = s.note,
            actualPickupTime = s.timeMillis,
        )
        _state.update { it.copy(justSaved = true) }
        return true
    }

    companion object {
        fun isLoggedLater(pickup: PickupEntity): Boolean =
            pickup.loggedAt - pickup.actualPickupTime > 60L * 60_000
    }
}
