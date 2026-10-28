package com.february10.driverradar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.february10.driverradar.R
import com.february10.driverradar.data.local.LocationEntity
import com.february10.driverradar.data.local.PickupEntity
import com.february10.driverradar.ui.viewmodel.LocationDetailViewModel
import com.february10.driverradar.ui.viewmodel.LogPickupViewModel
import com.february10.driverradar.util.formatWhen

/**
 * Per-location pickup history, newest actual pickup first, with delete + a
 * 5s undo window shown as a persistent "Deleted — Undo" bar pinned at the
 * bottom. Tapping a My Locations row opens this screen.
 */
@Composable
fun LocationDetailScreen(
    modifier: Modifier = Modifier,
    location: LocationEntity,
    viewModel: LocationDetailViewModel,
    onBack: () -> Unit,
    onLogHere: (LocationEntity) -> Unit,
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    // The undo bar stays up for ~5s after a delete. We track it locally so
    // the UI can re-insert on tap; the VM holds the actual row.
    var showUndo by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.location_detail_count, history.size),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            if (history.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.location_detail_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            items(history, key = { it.id }) { pickup ->
                PickupHistoryRow(
                    pickup = pickup,
                    loggedLater = LogPickupViewModel.isLoggedLater(pickup),
                    onDelete = {
                        viewModel.delete(pickup)
                        showUndo = true
                    },
                    onLogHere = { onLogHere(location) },
                )
            }
        }

        if (showUndo) {
            UndoBar(onUndo = {
                viewModel.undoDelete()
                showUndo = false
            })
        }
    }
}

/** Pinned "Deleted — Undo" bar. */
@Composable
private fun UndoBar(onUndo: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.location_detail_deleted),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onUndo) { Text(stringResource(R.string.common_undo)) }
        }
    }
}

@Composable
private fun PickupHistoryRow(
    pickup: PickupEntity,
    loggedLater: Boolean,
    onDelete: () -> Unit,
    onLogHere: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = pickup.customerAlias, style = MaterialTheme.typography.titleMedium)
                Text(text = formatWhen(pickup.actualPickupTime), style = MaterialTheme.typography.bodySmall)
                pickup.note?.takeIf { it.isNotEmpty() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
                if (loggedLater) {
                    Text(
                        text = "  (${stringResource(R.string.location_detail_logged_later)})",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            TextButton(onClick = onLogHere) { Text(stringResource(R.string.log_here)) }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.common_delete)) }
        }
    }
}
