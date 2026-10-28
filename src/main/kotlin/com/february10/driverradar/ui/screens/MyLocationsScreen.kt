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
import androidx.compose.material3.OutlinedTextField
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
import com.february10.driverradar.ui.viewmodel.MyLocationRow
import com.february10.driverradar.ui.viewmodel.MyLocationsViewModel
import java.util.Locale

/**
 * "My Locations" tab: saved locations ordered last-pickup-first with pickup
 * count + recency, plus a quick "add location manually" entry. Tapping a
 * row opens Location Detail (Phase 03 wires that; this tab lists today).
 */
@Composable
fun MyLocationsScreen(
    modifier: Modifier = Modifier,
    viewModel: MyLocationsViewModel,
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    var nameInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            // Manual add: name only for now (coordinates optional in the VM API).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.my_locations_add_manual)) },
                    singleLine = true,
                )
                TextButton(
                    onClick = {
                        viewModel.addManualLocation(nameInput)
                        nameInput = ""
                    },
                ) { Text(stringResource(R.string.my_locations_add)) }
            }
        }

        if (rows.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.my_locations_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        items(rows, key = { it.location.id }) { row ->
            LocationRow(row)
        }
    }
}

@Composable
private fun LocationRow(row: MyLocationRow) {
    Card {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.location.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = row.location.category,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = pickupCountLabel(row.pickupCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
                row.lastPickupTime?.let {
                    Text(
                        text = stringResource(R.string.my_locations_last, relativeTimeText(it)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun pickupCountLabel(count: Int): String =
    if (count == 1) "1 pickup" else "$count pickups"

/** Compact "just now / 2h / 3d ago" relative time, no external deps. */
private fun relativeTimeText(epochMillis: Long): String {
    val minutes = (System.currentTimeMillis() - epochMillis) / 60_000L
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}
