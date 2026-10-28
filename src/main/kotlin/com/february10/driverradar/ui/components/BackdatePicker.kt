package com.february10.driverradar.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.february10.driverradar.R
import com.february10.driverradar.util.formatWhen

/**
 * Quick time picker: default "now" with ±15min backdate chips and an
 * "Earlier…" button that expands to a full date+time picker. No keyboard.
 */
@Composable
fun BackdatePicker(
    selectedMillis: Long,
    nowMillis: Long,
    onChips: (Long) -> Unit,
    onFullPickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = selectedMillis == nowMillis,
            onClick = { onChips(nowMillis) },
            label = { Text("Now") },
        )
        FilterChip(
            selected = selectedMillis == nowMillis - 15 * 60_000L,
            onClick = { onChips(nowMillis - 15 * 60_000L) },
            label = { Text("15 min ago") },
        )
        TextButton(onClick = onFullPickerClick) {
            Text(stringResource(R.string.log_earlier, formatWhen(selectedMillis)))
        }
    }
}
