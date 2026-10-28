package com.february10.driverradar.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.february10.driverradar.R

private const val PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION

/**
 * ACCESS_FINE_LOCATION runtime flow (Phase 04 replaces the Phase 02
 * placeholder): one-shot request launcher, a rationale dialog while
 * [Activity.shouldShowRequestPermissionRationale] is true, and a settings
 * deep link when the user has hit "don't ask again".
 *
 * The returned function is what the "Allow location" button calls.
 *
 * @param onGranted permission just granted — caller re-runs its search.
 * @param onDenied  denied but re-askable — caller keeps the prompt up;
 *                  the last-known location (if any) remains the fallback.
 * @param onPermanentlyDenied "don't ask again" — caller offers
 *                  [openAppSettings] instead of re-requesting (a re-request
 *                  would be silently dropped by the OS).
 */
@Composable
fun rememberLocationPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onPermanentlyDenied: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    // shouldShowRequestPermissionRationale is Activity-only; the screen is
    // always composed under an Activity, but degrade gracefully if not.
    val activity = context as? Activity
    // Stable callbacks — the launcher registers once and outlives recomposition.
    val currentGranted = rememberUpdatedState(onGranted)
    val currentDenied = rememberUpdatedState(onDenied)
    val currentPermanent = rememberUpdatedState(onPermanentlyDenied)
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        when {
            granted -> currentGranted.value()
            // Rationale flag true = user denied once but hasn't checked
            // "don't ask again" — still re-askable.
            (activity?.shouldShowRequestPermissionRationale(PERMISSION) == true) -> currentDenied.value()
            else -> currentPermanent.value()
        }
    }

    val trigger: () -> Unit = {
        when {
            isPermissionGranted(context) -> currentGranted.value()
            activity?.shouldShowRequestPermissionRationale(PERMISSION) == true -> showRationale = true
            else -> launcher.launch(PERMISSION)
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.permission_rationale_title)) },
            text = { Text(stringResource(R.string.permission_rationale_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    launcher.launch(PERMISSION)
                }) { Text(stringResource(R.string.nearby_allow_location)) }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text(stringResource(R.string.common_later))
                }
            },
        )
    }
    return trigger
}

fun isPermissionGranted(context: Context): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(context, PERMISSION) ==
        PackageManager.PERMISSION_GRANTED

/** Settings deep link to this app's permission screen (the "don't ask again" fallback). */
fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
