package com.february10.driverradar.data.places

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Request bodies — Google Places API (New)
// ---------------------------------------------------------------------------

@Serializable
data class NearbySearchRequest(
    val locationBias: LatLng,
    val maxResultCount: Int,
    val rankPreference: String = "RANKING_PRECISE",
    val regionCode: String = "vn",
    val languageCode: String = "en",
    /** Google's Nearby Search (New) takes a list (max 5) of primary types. */
    val includedPrimaryTypes: List<String>? = null,
)

@Serializable
data class TextSearchRequest(
    val textQuery: String,
    val locationBias: LatLng,
    val regionCode: String = "vn",
    val languageCode: String = "en",
)

@Serializable
data class LatLng(val lat: Double, val lng: Double)

// ---------------------------------------------------------------------------
// Response bodies — Places API (New) field names. Google omits optional
// fields freely, so every field is nullable/defaulted; parsing uses
// ignoreUnknownKeys so Google can add fields without breaking us.
// ---------------------------------------------------------------------------

@Serializable
data class NearbySearchResponse(
    val places: List<Place> = emptyList(),
)

@Serializable
data class TextSearchResponse(
    val places: List<Place> = emptyList(),
)

@Serializable
data class Place(
    /** "name" is present but optional in the API; displayName is the stable field. */
    val displayName: DisplayName? = null,
    val id: String? = null,
    val location: PlaceLatLng? = null,
    /** Most-specific category, e.g. "restaurant". */
    val primaryType: String? = null,
) {
    val name: String get() = displayName?.text ?: "Place"
    /** Stable place id used for Location dedupe. */
    val placeId: String? get() = id
}

@Serializable
data class DisplayName(val text: String? = null, val languageCode: String? = null)

@Serializable
data class PlaceLatLng(val latitude: Double? = null, val longitude: Double? = null)
