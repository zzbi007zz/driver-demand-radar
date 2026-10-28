package com.february10.driverradar.repo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.february10.driverradar.data.local.LocationDao
import com.february10.driverradar.data.local.PickupDao
import com.february10.driverradar.model.ExportBackup
import com.february10.driverradar.model.ExportLocation
import com.february10.driverradar.model.ExportPickup
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.json.Json

/**
 * JSON backup: serializes both tables to a human-readable file in the cache
 * dir, shared deliberately via a FileProvider content URI + ACTION_SEND.
 * The only data path out of the phone — no background upload, no cloud.
 *
 * The written file lives in the cache dir on purpose: it is a transient
 * share payload, not a user document. [parse] supports the round-trip test
 * and a future import.
 */
class ExportRepository(
    appContext: Context,
    locationDao: LocationDao,
    pickupDao: PickupDao,
) {

    private val context = appContext
    private val locations = locationDao
    private val pickups = pickupDao
    private val dir: File = File(appContext.cacheDir, "exports")
    private val json = Json { prettyPrint = true; encodeDefaults = true; explicitNulls = true }

    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Writes the full backup to the cache dir and returns a content-URI the
     * caller hands to an [android.content.Intent.ACTION_SEND] chooser.
     * Throws on I/O failure — callers surface it as a toast/snackbar.
     */
    suspend fun export(): Uri {
        val backup = buildBackup()
        dir.mkdirs()
        val file = File(dir, "demand-radar-backup-${fileNameFormat.format(Date(backup.exportedAt))}.json")
        file.writeText(toJson(backup))
        return FileProvider.getUriForFile(context, "${context.packageName}.export", file)
    }

    /** Pretty JSON of a backup (public so unit tests can round-trip without Android I/O). */
    fun toJson(backup: ExportBackup): String =
        json.encodeToString(ExportBackup.serializer(), backup)

    /** Splits out so unit tests exercise the serialization without Android I/O. */
    suspend fun buildBackup(): ExportBackup = ExportBackup(
        locations = locations.all().map {
            ExportLocation(
                id = it.id,
                source = it.source,
                placeId = it.placeId,
                name = it.name,
                category = it.category,
                lat = it.lat,
                lng = it.lng,
                createdAt = it.createdAt,
            )
        },
        pickups = pickups.all().map {
            ExportPickup(
                id = it.id,
                locationId = it.locationId,
                customerAlias = it.customerAlias,
                note = it.note,
                loggedAt = it.loggedAt,
                actualPickupTime = it.actualPickupTime,
            )
        },
        exportedAt = System.currentTimeMillis(),
    )

    /** Parses a backup file back into its models (round-trip test / import later). */
    fun parse(jsonText: String): ExportBackup =
        json.decodeFromString(ExportBackup.serializer(), jsonText)
}
