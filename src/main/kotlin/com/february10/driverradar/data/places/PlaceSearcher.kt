package com.february10.driverradar.data.places

/**
 * Result of a place search: name, coordinates, an optional stable place id
 * (used for location dedupe) and an optional category.
 */
data class SearchablePlace(
    val name: String,
    val lat: Double,
    val lng: Double,
    val placeId: String? = null,
    val category: String? = null,
)

/**
 * A location search interface so Phase 02's real Places-backed searcher slots
 * in with zero refactor. [NoopPlaceSearcher] is the default until then.
 */
interface PlaceSearcher {
    /** Returns matching places for the query; empty when no results. */
    suspend fun search(query: String): List<SearchablePlace>
}

/** Placeholder searcher: emits nothing. Phase 02 replaces this. */
class NoopPlaceSearcher : PlaceSearcher {
    override suspend fun search(query: String): List<SearchablePlace> = emptyList()
}
