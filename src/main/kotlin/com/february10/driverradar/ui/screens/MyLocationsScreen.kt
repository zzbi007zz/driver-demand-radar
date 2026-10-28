package com.february10.driverradar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import com.february10.driverradar.data.local.LocationEntity
import com.february10.driverradar.ui.viewmodel.MyLocationsViewModel

/**
 * "My Locations" tab: lists saved locations (last-pickup-first).
 * The [MyLocationsViewModel] is built in [MainActivity] from the app
 * container and passed in; each row shows the name and category. Empty
 * state until any are saved.
 */
@Composable
fun MyLocationsScreen(
    modifier: Modifier = Modifier,
    viewModel: MyLocationsViewModel,
) {
    val locations by viewModel.locations.collectAsStateWithLifecycle()

    if (locations.isEmpty()) {
        EmptyState()
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(locations, key = { it.id }) { location ->
            LocationRow(location)
        }
    }
}

@Composable
private fun LocationRow(location: LocationEntity) {
    Card {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = location.category,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.my_locations_empty),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
