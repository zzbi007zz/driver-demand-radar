package com.february10.driverradar.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.february10.driverradar.BuildConfig
import com.february10.driverradar.R
import com.february10.driverradar.ui.viewmodel.SettingsViewModel
import com.february10.driverradar.util.formatWhen

/**
 * Settings tab: app metadata, heat-gate editor, and the user-initiated JSON
 * export (ACTION_SEND chooser). The Google-key status reflects the
 * resValue-injected key (empty on a clean checkout).
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val keySet = context.getString(R.string.GOOGLE_MAPS_API_KEY).isNotBlank()
    val gate by viewModel.gate.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card {
            Column(modifier = Modifier.padding(12.dp)) {
                Row {
                    Text(
                        text = stringResource(R.string.settings_app_version),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(text = BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Card {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.settings_google_key),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            if (keySet) R.string.settings_key_set
                            else R.string.settings_key_not_set
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Card {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.settings_heat_gate),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.settings_heat_gate_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Slider(
                        value = gate.toFloat(),
                        onValueChange = { viewModel.setGate(it.toInt()) },
                        valueRange = 5f..50f,
                        steps = 45, // integer stops 5..50 (45 segments, 46 stops)
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = gate.toString(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        ExportCard(
            lastExportAt = viewModel.lastExportAt,
            exportState = exportState,
            onExport = {
                scope.launch {
                    runCatching { viewModel.export() }
                        .onSuccess { uri -> shareExport(context, uri) }
                        .onFailure { /* failed state shown on the button via exportState */ }
                }
            },
        )
    }
}

/** ACTION_SEND chooser over the FileProvider content URI (write permission). */
private fun shareExport(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.settings_export)))
}

@Composable
private fun ExportCard(
    lastExportAt: Long?,
    exportState: SettingsViewModel.ExportState,
    onExport: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.settings_last_export),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = lastExportAt?.let { stringResource(R.string.settings_last_export_at, formatWhen(it)) }
                        ?: stringResource(R.string.settings_last_export_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(text = stringResource(R.string.settings_export_warning), style = MaterialTheme.typography.labelSmall)
            TextButton(
                onClick = onExport,
                enabled = exportState != SettingsViewModel.ExportState.InProgress,
            ) {
                Text(
                    text = when (exportState) {
                        SettingsViewModel.ExportState.InProgress -> stringResource(R.string.settings_exporting)
                        SettingsViewModel.ExportState.Exported -> stringResource(R.string.settings_export_done)
                        else -> stringResource(R.string.settings_export)
                    },
                )
            }
        }
    }
}
