package com.february10.driverradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State for the Nearby tab. Two phases:
 * - [NoApiKey]: the Maps key resValue is empty (clean checkout) — search disabled.
 * - [Ready]: a key is present; the real Places search UI ships in Phase 02.
 *
 * The key is passed in at construction (read from the resValue-generated string
 * resource by the caller) so the VM stays unit-testable with no Android context.
 */
class NearbyViewModel(private val googleMapsApiKey: String) : ViewModel() {

    private val _uiState = MutableStateFlow(
        if (googleMapsApiKey.isBlank()) UiState.NoApiKey else UiState.Ready
    )

    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    sealed interface UiState {
        /** No Google Maps API key configured yet. */
        data object NoApiKey : UiState

        /** Key present; places search not wired (Phase 02). */
        data object Ready : UiState
    }
}
