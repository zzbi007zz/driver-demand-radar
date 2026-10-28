package com.february10.driverradar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.february10.driverradar.R
import com.february10.driverradar.data.local.LocationCategory
import com.february10.driverradar.ui.viewmodel.NearbyViewModel
import com.february10.driverradar.util.openAppSettings
import com.february10.driverradar.util.rememberLocationPermission

/**
 * Nearby tab: category + radius chips, one-tap "Search" using GPS, a result
 * list with per-row "Save" actions (deduped by placeId into My Locations),
 * and honest states for no-key / no-GPS / empty / errors.
 *
 * Phase 04 replaces the Phase 02 permission placeholder: the NoPermission
 * row now carries an "Allow location" button (rationale dialog on 2nd
 * denial, settings deep link after "don't ask again"). A granted permission
 * re-runs the search; while re-askable, the last-known location stays the
 * fallback.
 */
@Composable
fun NearbyScreen(
    modifier: Modifier = Modifier,
    viewModel: NearbyViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.category.collectAsStateWithLifecycle()
    val selectedRadius by viewModel.radius.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Granted -> re-search (the user just allowed location). Re-askable
    // denial -> back to NoPermission. "Don't ask again" -> NoPermission with
    // the settings link surfaced by the VM's permanentDenied flag.
    val requestPermission = rememberLocationPermission(
        onGranted = { viewModel.search() },
        onDenied = { viewModel.locationPermissionDenied(reAskable = true) },
        onPermanentlyDenied = { viewModel.locationPermissionDenied(reAskable = false) },
    )

    LazyColumn(modifier = modifier.fillMaxSize().padding(12.dp)) {
        item {
            Text(text = stringResource(R.string.nearby_title), style = MaterialTheme.typography.titleMedium)
        }

        item {
            // Category chips.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LocationCategory.entries.forEach { category ->
                    FilterChip(
                        selected = category == selectedCategory,
                        onClick = { viewModel.category.value = category },
                        label = { Text(category.name.lowercase()) },
                    )
                }
            }
        }

        item {
            // Radius chips + search trigger.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NearbyViewModel.Radius.entries.forEach { r ->
                    FilterChip(
                        selected = r == selectedRadius,
                        onClick = { viewModel.radius.value = r },
                        label = { Text(r.label) },
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.search() }) {
                    Text(stringResource(R.string.nearby_search))
                }
            }
        }

        // Status line: loading / errors / permission issues.
        item {
            when (val s = state) {
                NearbyViewModel.UiState.Idle,
                NearbyViewModel.UiState.Locating -> Unit
                is NearbyViewModel.UiState.Loading ->
                    Text(stringResource(R.string.nearby_loading), modifier = Modifier.padding(top = 8.dp))
                is NearbyViewModel.UiState.NoPermission ->
                    PermissionPrompt(
                        permanent = s.permanent,
                        onAllow = requestPermission,
                        onOpenSettings = { openAppSettings(context) },
                    )
                NearbyViewModel.UiState.NoFix ->
                    Text(stringResource(R.string.nearby_no_fix), modifier = Modifier.padding(top = 8.dp))
                NearbyViewModel.UiState.NoKey ->
                    Text(stringResource(R.string.nearby_no_api_key), modifier = Modifier.padding(top = 8.dp))
                NearbyViewModel.UiState.ApiError ->
                    Text(stringResource(R.string.nearby_api_error), modifier = Modifier.padding(top = 8.dp))
                NearbyViewModel.UiState.NetworkError ->
                    Text(stringResource(R.string.nearby_network_error), modifier = Modifier.padding(top = 8.dp))
                is NearbyViewModel.UiState.Results ->
                    if (s.places.isEmpty()) Text(
                        text = stringResource(R.string.nearby_no_results),
                        modifier = Modifier.padding(top = 8.dp),
                    )
            }
        }

        // Result rows.
        val results = (state as? NearbyViewModel.UiState.Results)?.places ?: emptyList()
        items(results, key = { it.place.placeId ?: it.place.name }) { row ->
            PlaceResultCard(row = row, onSave = { viewModel.saveToMyLocations(row) })
        }
    }
}

/**
 * NoPermission state, made actionable. Re-askable: an "Allow location"
 * button triggers the request. Permanently denied: the same button path
 * opens system settings instead (a launcher would be silently ignored).
 */
@Composable
private fun PermissionPrompt(
    permanent: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (permanent) {
                    stringResource(R.string.permission_permanent_denied)
                } else {
                    stringResource(R.string.nearby_no_permission)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (permanent) {
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.permission_open_settings))
                }
            } else {
                Button(onClick = onAllow) {
                    Text(stringResource(R.string.nearby_allow_location))
                }
            }
        }
    }
}

@Composable
private fun PlaceResultCard(
    row: NearbyViewModel.ResultRow,
    onSave: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = row.place.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "${row.distanceMeters} m", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onSave, enabled = !row.saved) {
                Text(if (row.saved) stringResource(R.string.nearby_saved) else stringResource(R.string.nearby_save))
            }
        }
    }
}
