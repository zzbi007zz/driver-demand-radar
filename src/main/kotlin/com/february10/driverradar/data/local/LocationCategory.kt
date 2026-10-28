package com.february10.driverradar.data.local

/**
 * The location categories the driver cares about, mapped to their Google
 * Places API (New) request parameters.
 *
 * [apiPrimaryType] is null when Google's taxonomy has no discrete place type
 * for the category — RESIDENTIAL areas are not individual places — so those
 * searches route through text search with [textKeyword] instead of Nearby
 * Search's primaryType filter.
 */
enum class LocationCategory(val apiPrimaryType: String?, val textKeyword: String) {
    RESTAURANT("restaurant", "restaurant"),
    SCHOOL("school", "school"),
    OFFICE("office", "office"),
    RESIDENTIAL(null, "residential"),
}
