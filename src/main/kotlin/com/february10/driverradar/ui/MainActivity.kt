package com.february10.driverradar.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.february10.driverradar.DriverDemandRadarApplication
import com.february10.driverradar.R
import com.february10.driverradar.data.local.LocationEntity
import com.february10.driverradar.ui.screens.LocationDetailScreen
import com.february10.driverradar.ui.screens.LogPickupSheet
import com.february10.driverradar.ui.screens.MyLocationsScreen
import com.february10.driverradar.ui.screens.NearbyScreen
import com.february10.driverradar.ui.screens.SettingsScreen
import com.february10.driverradar.ui.viewmodel.LocationDetailViewModel
import com.february10.driverradar.ui.viewmodel.LogPickupViewModel
import com.february10.driverradar.ui.viewmodel.MyLocationsViewModel
import com.february10.driverradar.ui.viewmodel.NearbyViewModel

// Single-activity Compose shell: Material3 Scaffold, 3 bottom-nav tabs, and a
// floating "Log" FAB that opens the Log Pickup sheet. Location detail overlays
// the tabs; the log sheet overlays everything. Tab switching is a `when`.
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pull the hand-rolled DI root off the Application.
        val container = (application as DriverDemandRadarApplication).container

        val fabLogLabel = resources.getString(R.string.fab_log)
        val loggedText = resources.getString(R.string.log_saved_snackbar)

        setContent {
            MaterialTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                var selectedTab by remember { mutableIntStateOf(0) }

                // Overlay state: null = not shown.
                var detailLocation by remember { mutableStateOf<LocationEntity?>(null) }
                var logSheetOpen by remember { mutableStateOf(false) }
                var logSheetPrefill by remember { mutableStateOf<LocationEntity?>(null) }

                // Lifecycle-scoped VMs.
                val nearbyViewModel: NearbyViewModel = viewModel {
                    NearbyViewModel(
                        placeSearcher = container.placeSearcher,
                        gpsProvider = container.gpsProvider,
                        locationRepository = container.locationRepository,
                    )
                }
                val myLocationsViewModel: MyLocationsViewModel = viewModel {
                    MyLocationsViewModel(container.locationRepository)
                }
                val logViewModel: LogPickupViewModel = viewModel {
                    LogPickupViewModel(container.locationRepository, container.pickupRepository)
                }
                // One detail VM per opened location; a new location = a new VM.
                val detailViewModel = remember(detailLocation) {
                    LocationDetailViewModel(container.pickupRepository, detailLocation?.id ?: 0L)
                }

                // Reset the sheet for a fresh open (prefilled or clean).
                LaunchedEffect(logSheetOpen) {
                    if (logSheetOpen) logViewModel.start(logSheetPrefill)
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            logSheetPrefill = null
                            logSheetOpen = true
                        }) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = fabLogLabel)
                        }
                    },
                    bottomBar = {
                        NavigationBar {
                            NavTab(0, Icons.Filled.Home, R.string.tab_nearby, selectedTab) { selectedTab = it }
                            NavTab(1, Icons.Filled.Place, R.string.tab_my_locations, selectedTab) { selectedTab = it }
                            NavTab(2, Icons.Filled.Settings, R.string.tab_settings, selectedTab) { selectedTab = it }
                        }
                    },
                ) { padding ->
                    when (selectedTab) {
                        1 -> MyLocationsScreen(
                            modifier = Modifier.padding(padding),
                            viewModel = myLocationsViewModel,
                            onLocationClick = { detailLocation = it },
                        )
                        2 -> SettingsScreen(Modifier.padding(padding))
                        else -> NearbyScreen(Modifier.padding(padding), nearbyViewModel)
                    }
                }

                // Location detail overlays the tab content.
                detailLocation?.let { location ->
                    LocationDetailScreen(
                        location = location,
                        viewModel = detailViewModel,
                        onBack = { detailLocation = null },
                        onLogHere = { target ->
                            logSheetPrefill = target
                            logSheetOpen = true
                        },
                    )
                }

                // "Logged" confirmation snackbar, fired on the scaffold host
                // (the sheet auto-dismisses on save, so the trigger lives here).
                LogSavedEffect(logViewModel, snackbarHostState, loggedText)

                // Log pickup sheet overlays everything.
                if (logSheetOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { logSheetOpen = false },
                    ) {
                        LogPickupSheet(viewModel = logViewModel, onDismiss = { logSheetOpen = false })
                    }
                }
            }
        }
    }
}

/** Fires the "logged" snackbar exactly when a save lands. */
@Composable
private fun LogSavedEffect(
    viewModel: LogPickupViewModel,
    snackbarHostState: SnackbarHostState,
    message: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.justSaved) {
        if (state.justSaved) {
            snackbarHostState.showSnackbar(message)
        }
    }
}

// Bottom-nav item; RowScope extension: NavigationBarItem lives in NavigationBar's
// RowScope in Material3 1.4+.
@Composable
private fun RowScope.NavTab(
    id: Int,
    icon: ImageVector,
    labelRes: Int,
    selectedTab: Int,
    onSelected: (Int) -> Unit,
) {
    NavigationBarItem(
        selected = selectedTab == id,
        onClick = { onSelected(id) },
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(stringResource(labelRes)) },
    )
}
