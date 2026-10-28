package com.february10.driverradar.data.places

import com.february10.driverradar.data.local.LocationCategory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Places API (New) implementation of [PlaceSearcher] over OkHttp.
 *
 * Routes per category: a non-null [LocationCategory.apiPrimaryType] uses Nearby
 * Search (`/v1/places:searchNearby`); otherwise (RESIDENTIAL) it falls back
 * to Text Search (`/v1/places:searchText`) with the category's keyword. Both
 * bias results around the caller's GPS point.
 *
 * The API key comes from the resValue-injected string (empty on a clean
 * checkout → every call short-circuits to [SearchOutcome.NoKey]).
 */
class GooglePlaceSearcher(private val apiKey: String) : PlaceSearcher {

    private val json: Json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun nearby(
        category: LocationCategory,
        lat: Double,
        lng: Double,
        radiusMeters: Int,
    ): SearchOutcome {
        if (apiKey.isBlank()) return SearchOutcome.NoKey
        val bias = LatLng(lat, lng)

        val response = if (category.apiPrimaryType != null) {
            val body = json.encodeToString(
                NearbySearchRequest.serializer(),
                NearbySearchRequest(
                    locationBias = bias,
                    maxResultCount = 20,
                    includedPrimaryTypes = listOf(category.apiPrimaryType),
                ),
            )
            http.newCall(
                Request.Builder()
                    .url(NEARBY_ENDPOINT)
                    .post(body.toRequestBody(JSON_MEDIA))
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .build()
            ).execute()
        } else {
            val body = json.encodeToString(
                TextSearchRequest.serializer(),
                TextSearchRequest(textQuery = category.textKeyword, locationBias = bias),
            )
            http.newCall(
                Request.Builder()
                    .url(TEXT_ENDPOINT)
                    .post(body.toRequestBody(JSON_MEDIA))
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .build()
            ).execute()
        }

        return try {
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return SearchOutcome.ApiError(response.code, text.take(200))

            // Both endpoints share the same "places" envelope shape for our
            // field mask, so parse through the shared list.
            val places = if (category.apiPrimaryType != null) {
                json.decodeFromString(NearbySearchResponse.serializer(), text).places
            } else {
                json.decodeFromString(TextSearchResponse.serializer(), text).places
            }
            SearchOutcome.Places(
                places.mapNotNull { place ->
                    val loc = place.location ?: return@mapNotNull null
                    val la = loc.latitude ?: return@mapNotNull null
                    val lo = loc.longitude ?: return@mapNotNull null
                    SearchablePlace(
                        name = place.name,
                        lat = la,
                        lng = lo,
                        placeId = place.placeId,
                        category = category.name.lowercase(),
                    )
                }
            )
        } catch (e: java.io.IOException) {
            SearchOutcome.NetworkError(e.message ?: "network error")
        } finally {
            response.close()
        }
    }

    private companion object {
        const val BASE = "https://places.googleapis.com"
        const val NEARBY_ENDPOINT = "$BASE/v1/places:searchNearby"
        const val TEXT_ENDPOINT = "$BASE/v1/places:searchText"
        const val FIELD_MASK =
            "places.displayName,places.id,places.location,places.primaryType"
        val JSON_MEDIA = "application/json".toMediaType()
    }
}
