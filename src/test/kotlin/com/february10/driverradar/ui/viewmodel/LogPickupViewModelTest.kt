package com.february10.driverradar.ui.viewmodel

import com.february10.driverradar.data.local.PickupEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-function tests for the log-sheet logic (no Android needed):
 * the "logged later" badge boundary.
 */
class LogPickupViewModelTest {

    private fun pickup(loggedAt: Long, actual: Long) = PickupEntity(
        locationId = 1L,
        customerAlias = "x",
        note = null,
        loggedAt = loggedAt,
        actualPickupTime = actual,
    )

    @Test
    fun sameInstantIsNotLoggedLater() {
        assertFalse(LogPickupViewModel.isLoggedLater(pickup(1000L, 1000L)))
    }

    @Test
    fun backdateWithinOneHourIsNotLoggedLater() {
        // Picked up 30 min before logging: normal fast logging.
        assertFalse(LogPickupViewModel.isLoggedLater(pickup(1_800_000L, 300_000L)))
    }

    @Test
    fun backdateJustOverOneHourFlagsLoggedLater() {
        // 1h + 1s gap crosses the boundary.
        assertTrue(LogPickupViewModel.isLoggedLater(pickup(3_601_000L, 0L)))
    }

    @Test
    fun nextDayLogIsLoggedLater() {
        val aDay = 24 * 60 * 60_000L
        assertTrue(LogPickupViewModel.isLoggedLater(pickup(10_000L + aDay, 10_000L)))
    }
}
