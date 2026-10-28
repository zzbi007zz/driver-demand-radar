package com.february10.driverradar.model

import kotlinx.serialization.Serializable

/**
 * JSON backup format (the only data path out of the phone). Field names
 * match the Room column names one-for-one, so a backup reads like a dump of
 * the two tables plus an `exportedAt` stamp. No PII by design: aliases and
 * notes are caller-chosen free text.
 */
@Serializable
data class ExportBackup(
    val locations: List<ExportLocation>,
    val pickups: List<ExportPickup>,
    /** UTC epoch millis when the export was written. */
    val exportedAt: Long,
)

@Serializable
data class ExportLocation(
    val id: Long,
    val source: String,
    /** Null for manual map taps (encoded explicitly so importers can tell). */
    val placeId: String?,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val createdAt: Long,
)

@Serializable
data class ExportPickup(
    val id: Long,
    val locationId: Long,
    val customerAlias: String,
    val note: String?,
    val loggedAt: Long,
    val actualPickupTime: Long,
)
