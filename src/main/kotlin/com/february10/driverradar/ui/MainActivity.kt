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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.february10.driverradar.DriverDemandRadarApplication
import com.february10.driverradar.R
import com.february10.driverradar.ui.screens.MyLocationsScreen
import com.february10.driverradar.ui.screens.NearbyScreen
import com.february10.driverradar.ui.screens.SettingsScreen
import com.february10.driverradar.ui.viewmodel.MyLocationsViewModel
import com.february10.driverradar.ui.viewmodel.NearbyViewModel

// Single-activity Compose shell: Material3 Scaffold, 3 bottom-nav tabs, and a
// floating "Log" FAB (Phase 03 placeholder -> snackbar); tab switching is a `when`.
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pull the hand-rolled DI root off the Application.
        val container = (application as DriverDemandRadarApplication).container

        // Non-composable reads so the Strings survive into the onClick lambdas.
        val fabLogLabel = resources.getString(R.string.fab_log)
        val fabSnackbarText = resources.getString(R.string.fab_snackbar_placeholder)

        setContent {
            MaterialTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val snackbarMessage = remember { mutableStateOf<String?>(null) }
                var selectedTab by remember { mutableIntStateOf(0) }

                LaunchedEffect(snackbarMessage.value) {
                    snackbarMessage.value?.let {
                        snackbarHostState.showSnackbar(it)
                        snackbarMessage.value = null
                    }
                }

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

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { snackbarMessage.value = fabSnackbarText }) {
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
                        1 -> MyLocationsScreen(Modifier.padding(padding), myLocationsViewModel)
                        2 -> SettingsScreen(Modifier.padding(padding))
                        else -> NearbyScreen(Modifier.padding(padding), nearbyViewModel)
                    }
                }
            }
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
