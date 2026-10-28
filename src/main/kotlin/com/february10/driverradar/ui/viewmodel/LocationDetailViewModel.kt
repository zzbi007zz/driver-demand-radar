package com.february10.driverradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.february10.driverradar.data.local.PickupEntity
import com.february10.driverradar.model.HeatMatrix
import com.february10.driverradar.repo.HeatRepository
import com.february10.driverradar.repo.PickupRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Per-location pickup history (newest actual pickup first) with delete + a
 * short undo window, plus the 7x24 [HeatMatrix] for the detail screen's heat
 * strip. The "logged later" badge is a pure function of the row's
 * loggedAt vs actualPickupTime (see [LogPickupViewModel.isLoggedLater]).
 *
 * The deleted row is held in [pendingUndo] for ~5s; [undoDelete] re-inserts
 * it with its original id/time. No tombstones — local-only scope.
 */
class LocationDetailViewModel(
    private val pickupRepository: PickupRepository,
    val heatRepository: HeatRepository,
    private val locationId: Long,
) : ViewModel() {

    val history: StateFlow<List<PickupEntity>> =
        pickupRepository.historyByLocation(locationId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** 7x24 heat for this location; the UI applies the min-count gate. */
    val heat: StateFlow<HeatMatrix> =
        heatRepository.matrixFor(locationId)
            .stateIn(viewModelScope, SharingStarted.Lazily, HeatMatrix(locationId, emptyList(), 0))

    /** Last-deleted row, retained briefly so the undo affordance can restore it. */
    var pendingUndo: PickupEntity? = null
        private set

    fun delete(pickup: PickupEntity) {
        viewModelScope.launch {
            pickupRepository.delete(pickup.id)
            // Retained only once the delete is durable; cleared on undo.
            pendingUndo = pickup
        }
    }

    /** Re-inserts the last-deleted row with its original id/times. No-op if none. */
    fun undoDelete() {
        val row = pendingUndo ?: return
        pendingUndo = null
        viewModelScope.launch {
            pickupRepository.restore(row)
        }
    }

    fun edit(pickup: PickupEntity) {
        viewModelScope.launch {
            pickupRepository.update(pickup)
        }
    }
}
