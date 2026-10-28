package com.february10.driverradar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.february10.driverradar.R
import com.february10.driverradar.ui.viewmodel.NearbyViewModel

/**
 * Nearby tab: surfaces "where should I be picking up next".
 *
 * Phase 01 shows the two static states (no key / key present) that gate the
 * Phase 02 places-search UI. The [NearbyViewModel] is built in
 * [com.february10.driverradar.ui.MainActivity] from the resValue-injected
 * [R.string.GOOGLE_MAPS_API_KEY] (empty on a clean checkout).
 */
@Composable
fun NearbyScreen(
    modifier: Modifier = Modifier,
    viewModel: NearbyViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val message = when (state) {
        NearbyViewModel.UiState.NoApiKey -> R.string.nearby_no_api_key
        NearbyViewModel.UiState.Ready -> R.string.nearby_placeholder
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
