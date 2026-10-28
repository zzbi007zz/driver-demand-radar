package com.february10.driverradar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.february10.driverradar.BuildConfig
import com.february10.driverradar.R

/**
 * Settings tab: app metadata + a preview of the export feature that lands later.
 * The Google-key status reflects the resValue-injected key (empty on a clean
 * checkout). No ViewModel: the content is static apart from the version string.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val keySet = context.getString(R.string.GOOGLE_MAPS_API_KEY).isNotBlank()

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
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
                Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.settings_last_export),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_last_export_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
