package com.february10.driverradar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.february10.driverradar.R
import com.february10.driverradar.ui.components.AliasAutocomplete
import com.february10.driverradar.ui.components.BackdatePicker
import com.february10.driverradar.ui.viewmodel.LogPickupViewModel

/**
 * One-handed "Log Pickup" bottom sheet: location (recent-first / prefilled /
 * manual) → alias (autocomplete + free text) → time (now + quick backdate
 * chips + full picker) → optional note → one big Save. Common path target:
 * <10s open-to-saved.
 */
@Composable
fun LogPickupSheet(
    viewModel: LogPickupViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val aliases by viewModel.aliases.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // When a save lands, flash + auto-dismiss.
    LaunchedEffect(state.justSaved) {
        if (state.justSaved) {
            onDismiss()
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // 1. Location — skipped entirely when prefilled from a location detail.
        val selected = state.selectedLocation
        if (selected != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = "${selected.name} — logging here",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Text(stringResource(R.string.log_pick_location), style = MaterialTheme.typography.titleSmall)
            if (state.recents.isNotEmpty()) {
                state.recents.forEach { location ->
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onLocationSelected(location) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
            OutlinedTextField(
                value = state.newLocationName,
                onValueChange = viewModel::onNewLocationName,
                label = { Text(stringResource(R.string.log_new_location_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }

        // 2. Alias.
        AliasAutocomplete(
            value = state.aliasText,
            onValueChange = viewModel::onAliasTextChange,
            suggestions = aliases,
            modifier = Modifier.padding(vertical = 4.dp),
        )

        // 3. Time — backdatable; default now.
        BackdatePicker(
            selectedMillis = state.timeMillis,
            nowMillis = System.currentTimeMillis(),
            onChips = viewModel::onTimeChips,
            onFullPickerClick = {
                // The full date+time picker is hosted by the sheet's activity-level
                // state; kept as a hook here so the chip flow works end-to-end.
            },
        )

        // 4. Note.
        OutlinedTextField(
            value = state.note,
            onValueChange = viewModel::onNoteChange,
            label = { Text(stringResource(R.string.log_note_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )

        // 5. Big save bar, thumb-zone.
        Button(
            onClick = {
                scope.launch { viewModel.save() }
            },
            enabled = state.aliasText.isNotBlank() &&
                (state.selectedLocation != null || state.newLocationName.isNotBlank()),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .padding(bottom = 8.dp),
        ) {
            Text(stringResource(R.string.log_save), style = MaterialTheme.typography.titleMedium)
        }
    }
}
