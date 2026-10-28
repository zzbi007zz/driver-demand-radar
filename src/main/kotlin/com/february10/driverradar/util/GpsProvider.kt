package com.february10.driverradar.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.tasks.await

/** Result of a one-shot location read. */
sealed interface GpsResult {
    data class Fix(val lat: Double, val lng: Double) : GpsResult
    /** Location permission not granted — UI shows a "allow location" prompt. */
    data object NoPermission : GpsResult
    /** Permission granted but no known location. */
    data object NoFix : GpsResult
}

/**
 * One-shot GPS read for the Nearby tab. Uses FusedLocation's last-known
 * location — for a driver who is physically at a spot, that is usually
 * accurate and instant (no network round-trip). The full runtime-permission
 * *request* flow lands in Phase 04; today a missing permission just yields
 * [GpsResult.NoPermission] and the screen surfaces it.
 *
 * Note: play-services-location 19.x exposes the client via its constructor,
 * not the removed `getInstance` static.
 */
class GpsProvider(appContext: Context) {

    private val appContext: Context = appContext.applicationContext
    private val fusedClient: FusedLocationProviderClient =
        FusedLocationProviderClient(appContext)

    suspend fun getCurrentPosition(): GpsResult {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return GpsResult.NoPermission

        val lastKnown = try {
            fusedClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
        return if (lastKnown != null) GpsResult.Fix(lastKnown.latitude, lastKnown.longitude)
        else GpsResult.NoFix
    }
}
