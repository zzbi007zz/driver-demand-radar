package com.february10.driverradar.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.february10.driverradar.R
import com.february10.driverradar.data.local.PickupEntity
import com.february10.driverradar.util.formatWhen

/**
 * One pickup in a location's history: alias, actual time, optional note, a
 * "logged later" badge (when the entry was backdated), and Log/Delete
 * actions.
 */
@Composable
fun PickupHistoryRow(
    pickup: PickupEntity,
    loggedLater: Boolean,
    onDelete: () -> Unit,
    onLogHere: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
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

/** Pinned "Deleted — Undo" bar shown for ~5s after a history delete. */
@Composable
fun UndoBar(onUndo: () -> Unit) {
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
