package com.february10.driverradar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.february10.driverradar.R
import com.february10.driverradar.model.HeatMatrix

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private val HeatColor = Color(0xFF1565C0)
private val HeatEmpty = Color(0x1F1565C0)

/**
 * 7x24 demand heat: busiest slots as text rows + a single Canvas grid
 * (weekday rows x 24 hour columns, intensity = count/max). Tapping a cell
 * selects it — the host screen filters its pickup list to that slot.
 *
 * The below-gate "insufficient data" state is the screen's job; this
 * composable only renders a grid from real data.
 */
@Composable
fun HeatStrip(
    matrix: HeatMatrix,
    selected: Pair<Int, Int>?,
    onCellTap: (weekday: Int, hour: Int) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cells = matrix.buckets.associate { it.weekday * 24 + it.hour to it.count }
    val max = cells.values.maxOrNull() ?: 1

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.heat_title), style = MaterialTheme.typography.titleMedium)

            matrix.topSlots().forEach { bucket ->
                Text(
                    text = stringResource(
                        R.string.heat_top_slot,
                        DAY_LABELS[bucket.weekday],
                        bucket.hour,
                        bucket.hour + 1,
                        bucket.count,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (selected != null) {
                val cellCount = cells[selected.first * 24 + selected.second] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            R.string.heat_filtered,
                            cellCount,
                            DAY_LABELS[selected.first],
                            selected.second,
                            selected.second + 1,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearFilter) { Text(stringResource(R.string.heat_clear)) }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Weekday labels, one per grid row.
                Column(Modifier.width(30.dp)) {
                    repeat(7) {
                        Text(
                            text = DAY_LABELS[it],
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                HeatGridCanvas(
                    counts = cells,
                    max = max,
                    selected = selected,
                    onCellTap = onCellTap,
                )
            }
        }
    }
}

/** 7x24 Canvas; one Canvas instead of 168 composables keeps the tree flat. */
@Composable
private fun HeatGridCanvas(
    counts: Map<Int, Int>,
    max: Int,
    selected: Pair<Int, Int>?,
    onCellTap: (weekday: Int, hour: Int) -> Unit,
) {
    val cell = 11.dp
    val gap = 2.dp
    val density = LocalDensity.current
    val cellPx = with(density) { cell.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val gridWidth = with(density) { (cell + gap) * 24 - gap }
    val gridHeight = with(density) { (cell + gap) * 7 - gap }

    Canvas(
        modifier = Modifier
            .size(gridWidth, gridHeight)
            .pointerInput(selected, max) {
                detectTapGestures { offset ->
                    val hour = ((offset.x / (cellPx + gapPx)).toInt()).coerceIn(0, 23)
                    val weekday = ((offset.y / (cellPx + gapPx)).toInt()).coerceIn(0, 6)
                    onCellTap(weekday, hour)
                }
            },
    ) {
        for (weekday in 0..6) {
            for (hour in 0..23) {
                val x = hour * (cellPx + gapPx)
                val y = weekday * (cellPx + gapPx)
                val count = counts[weekday * 24 + hour] ?: 0
                drawRoundRect(
                    color = if (count == 0) HeatEmpty else HeatColor.copy(alpha = 0.2f + 0.8f * count / max),
                    topLeft = Offset(x, y),
                    size = Size(cellPx, cellPx),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
                if (selected == weekday to hour) {
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(x, y),
                        size = Size(cellPx, cellPx),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                        style = Stroke(2.dp.toPx()),
                    )
                }
            }
        }
    }
}
