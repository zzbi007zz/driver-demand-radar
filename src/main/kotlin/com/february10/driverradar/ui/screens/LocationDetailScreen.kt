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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.february10.driverradar.model.HeatBucket
import com.february10.driverradar.model.HeatMatrix
import com.february10.driverradar.repo.HeatRepository
import com.february10.driverradar.ui.components.HeatStrip
import com.february10.driverradar.ui.components.PickupHistoryRow
import com.february10.driverradar.ui.components.UndoBar
import com.february10.driverradar.ui.viewmodel.LocationDetailViewModel
import com.february10.driverradar.ui.viewmodel.LogPickupViewModel

/**
 * Per-location pickup history, newest actual pickup first, with delete + a
 * 5s undo bar pinned at the bottom. Tapping a My Locations row opens this
 * screen.
 *
 * [minHeatCount] is the heat gate: below it the card shows an "N of gate"
 * progress state instead of a grid — never a fake heat. Tapping a grid cell
 * filters the history list to that weekday+hour.
 */
@Composable
fun LocationDetailScreen(
    modifier: Modifier = Modifier,
    location: LocationEntity,
    viewModel: LocationDetailViewModel,
    minHeatCount: Int,
    onBack: () -> Unit,
    onLogHere: (LocationEntity) -> Unit,
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val heat by viewModel.heat.collectAsStateWithLifecycle()
    var showUndo by remember { mutableStateOf(false) }
    // Tapped heat cell (weekday, hour) or null; filters the history list.
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val windowStart = remember { HeatRepository.windowStartMillis() }

    val visibleHistory = selectedCell?.let { cell ->
        history.filter {
            it.actualPickupTime >= windowStart &&
                HeatBucket.of(it.actualPickupTime).weekday == cell.first &&
                HeatBucket.of(it.actualPickupTime).hour == cell.second
        }
    } ?: history

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
                HeatSection(
                    matrix = heat,
                    minHeatCount = minHeatCount,
                    selected = selectedCell,
                    onCellTap = { weekday, hour ->
                        selectedCell = if (selectedCell == weekday to hour) null else weekday to hour
                    },
                    onClearFilter = { selectedCell = null },
                )
            }

            item {
                Text(
                    text = stringResource(R.string.location_detail_count, visibleHistory.size),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            if (visibleHistory.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.location_detail_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            items(visibleHistory, key = { it.id }) { pickup ->
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

/** Heat card: grid when the gate is met, honest progress state below it. */
@Composable
private fun HeatSection(
    matrix: HeatMatrix,
    minHeatCount: Int,
    selected: Pair<Int, Int>?,
    onCellTap: (Int, Int) -> Unit,
    onClearFilter: () -> Unit,
) {
    if (matrix.totalCount >= minHeatCount) {
        HeatStrip(
            matrix = matrix,
            selected = selected,
            onCellTap = onCellTap,
            onClearFilter = onClearFilter,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    } else {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        R.string.heat_insufficient,
                        matrix.totalCount.coerceAtMost(minHeatCount),
                        minHeatCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { (matrix.totalCount.toFloat() / minHeatCount).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
