package com.february10.driverradar.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.february10.driverradar.repo.ExportRepository
import com.february10.driverradar.repo.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Settings tab: the heat-gate preference (persisted in [SettingsStore]) and
 * the user-initiated JSON export, shared via a FileProvider content URI.
 * No Android context in the class — [export] returns the URI for the
 * Compose layer to wire into the ACTION_SEND chooser (unit-testable).
 */
class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val exportRepository: ExportRepository,
) : ViewModel() {

    enum class ExportState { Idle, InProgress, Exported, Failed }

    val gate = MutableStateFlow(settingsStore.minHeatCount)

    /** Gate clamps 5..50 per spec; the store enforces it too. */
    fun setGate(value: Int) {
        val clamped = value.coerceIn(5, 50)
        gate.value = clamped
        settingsStore.minHeatCount = clamped
    }

    /** Last successful export (epoch millis), null = never. */
    val lastExportAt: Long? get() = settingsStore.lastExportAt

    private val _exportState = MutableStateFlow(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    /**
     * Writes the backup and stamps [lastExportAt]; returns the content URI
     * for the chooser. I/O failures flip [exportState] to Failed and
     * rethrow so the caller can surface a message.
     */
    suspend fun export(): Uri {
        _exportState.value = ExportState.InProgress
        return try {
            val uri = exportRepository.export()
            settingsStore.lastExportAt = System.currentTimeMillis()
            _exportState.value = ExportState.Exported
            uri
        } catch (e: Exception) {
            _exportState.value = ExportState.Failed
            throw e
        }
    }
}
