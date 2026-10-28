package com.february10.driverradar.data.places

import com.february10.driverradar.data.local.LocationCategory

/**
 * A location hit from a places search: name, coordinates, an optional stable
 * place id (used for location dedupe) and an optional category label.
 */
data class SearchablePlace(
    val name: String,
    val lat: Double,
    val lng: Double,
    val placeId: String? = null,
    val category: String? = null,
)

/**
 * Structured search outcome so the UI can show meaningful states instead of
 * throwing. [NoKey] = the Maps key resValue is empty (clean checkout).
 */
sealed interface SearchOutcome {
    /** One or more matches (possibly zero rows in the range). */
    data class Places(val results: List<SearchablePlace>) : SearchOutcome

    /** No Maps API key configured. */
    data object NoKey : SearchOutcome

    /** Google rejected the request (bad key, quota, IP restriction). */
    data class ApiError(val status: Int, val message: String) : SearchOutcome

    /** Could not reach Google at all. */
    data class NetworkError(val message: String) : SearchOutcome
}

/**
 * Category-nearby place search seam. Phase 01 shipped [NoopPlaceSearcher] so
 * the app builds without a key; Phase 02 backs it with the Places API (New).
 * [NoopPlaceSearcher] stays for unit tests.
 */
interface PlaceSearcher {
    /**
     * Search [radiusMeters] around the point for [category]. Categories with
     * no Google primary type (e.g. RESIDENTIAL) fall back to a text search.
     */
    suspend fun nearby(
        category: LocationCategory,
        lat: Double,
        lng: Double,
        radiusMeters: Int,
    ): SearchOutcome
}

/** Placeholder searcher: always reports "no key" so the UI gate is testable. */
class NoopPlaceSearcher : PlaceSearcher {
    override suspend fun nearby(
        category: LocationCategory,
        lat: Double,
        lng: Double,
        radiusMeters: Int,
    ): SearchOutcome = SearchOutcome.NoKey
}
