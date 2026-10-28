package com.february10.driverradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.february10.driverradar.data.local.LocationCategory
import com.february10.driverradar.data.places.PlaceSearcher
import com.february10.driverradar.data.places.SearchOutcome
import com.february10.driverradar.data.places.SearchablePlace
import com.february10.driverradar.repo.LocationRepository
import com.february10.driverradar.util.GpsProvider
import com.february10.driverradar.util.GpsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State for the Nearby tab. Flow: pick a [LocationCategory] + radius →
 * one-shot GPS → Places search (nearby or text, per category) → result list
 * with a "save to My Locations" action per row.
 *
 * [GpsProvider] and [PlaceSearcher] are injected so this stays unit-testable
 * with fakes; no Android context needed here.
 */
class NearbyViewModel(
    private val placeSearcher: PlaceSearcher,
    private val gpsProvider: GpsProvider,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    enum class Radius(val value: Int) {
        METERS_300(300), METERS_1000(1000), METERS_3000(3000);

        val label: String
            get() = when (this) {
                METERS_300 -> "300m"
                METERS_1000 -> "1 km"
                METERS_3000 -> "3 km"
            }
    }

    sealed interface UiState {
        data object Idle : UiState
        data object Locating : UiState
        data object NoPermission : UiState
        data object NoFix : UiState
        data object NoKey : UiState
        data class Loading(val category: LocationCategory) : UiState
        data object ApiError : UiState
        data object NetworkError : UiState
        data class Results(
            val category: LocationCategory,
            val places: List<ResultRow>,
        ) : UiState
    }

    /** A search hit plus how far it is from the GPS fix. */
    data class ResultRow(
        val place: SearchablePlace,
        val distanceMeters: Int,
        val saved: Boolean = false,
    )

    var category = MutableStateFlow(LocationCategory.RESTAURANT)
    var radius = MutableStateFlow(Radius.METERS_1000)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Triggers a fresh search with the currently selected category + radius. */
    fun search() {
        val cat = category.value
        val rad = radius.value
        _uiState.value = UiState.Loading(cat)
        viewModelScope.launch {
            val gps = gpsProvider.getCurrentPosition()
            val (lat, lng) = when (gps) {
                is GpsResult.NoPermission -> {
                    _uiState.value = UiState.NoPermission
                    return@launch
                }
                is GpsResult.NoFix -> {
                    _uiState.value = UiState.NoFix
                    return@launch
                }
                is GpsResult.Fix -> gps.lat to gps.lng
            }

            when (val outcome = placeSearcher.nearby(cat, lat, lng, rad.value)) {
                is SearchOutcome.NoKey ->
                    _uiState.value = UiState.NoKey
                is SearchOutcome.ApiError ->
                    _uiState.value = UiState.ApiError
                is SearchOutcome.NetworkError ->
                    _uiState.value = UiState.NetworkError
                is SearchOutcome.Places -> {
                    val rows = outcome.results.map { place ->
                        ResultRow(
                            place = place,
                            distanceMeters = distanceMeters(lat, lng, place.lat, place.lng),
                        )
                    }
                    _uiState.value = UiState.Results(cat, rows)
                }
            }
        }
    }

    /** Persists a hit to My Locations (deduped by placeId in the repository). */
    fun saveToMyLocations(row: ResultRow) {
        viewModelScope.launch {
            locationRepository.upsert(locationRepository.toEntity(row.place))
            // Flip the row's saved flag so the UI can reflect it immediately.
            val current = _uiState.value
            if (current is UiState.Results) {
                _uiState.value = current.copy(
                    places = current.places.map { if (it.place == row.place) it.copy(saved = true) else it },
                )
            }
        }
    }

    private fun distanceMeters(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Int {
        // Haversine, meters. Good enough for distance chips; no GeoUtils dep.
        val r = 6_371_000.0
        val dLat = Math.toRadians(bLat - aLat)
        val dLng = Math.toRadians(bLng - aLng)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(aLat)) * cos(Math.toRadians(bLat)) * sin(dLng / 2) * sin(dLng / 2)
        return (2 * r * asin(sqrt(h))).toInt()
    }

    private companion object {
        fun sin(v: Double) = kotlin.math.sin(v)
        fun cos(v: Double) = kotlin.math.cos(v)
        fun asin(v: Double) = kotlin.math.asin(v)
        fun sqrt(v: Double) = kotlin.math.sqrt(v)
    }
}
