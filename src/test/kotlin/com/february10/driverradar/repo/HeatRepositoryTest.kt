package com.february10.driverradar.repo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.february10.driverradar.data.local.DemandRadarDb
import com.february10.driverradar.data.local.LocationEntity
import com.february10.driverradar.data.local.LocationSource
import com.february10.driverradar.data.local.PickupEntity
import com.february10.driverradar.model.HeatMatrix
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Heat bucketing + min-count gate, JVM (Room in-memory under Robolectric).
 *
 * Buckets are computed in SQLite via strftime(..., 'localtime'), so the
 * expected values are derived with Calendar in the same JVM-local timezone
 * — both sides agree by construction. All fixtures are placed within the
 * trailing 8-week window (now minus N days) so the tests stay valid as
 * "today" moves.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HeatRepositoryTest {

    private lateinit var db: DemandRadarDb
    private lateinit var heat: HeatRepository
    private var locationId: Long = 0L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = DemandRadarDb.inMemory(context)
        heat = HeatRepository(db.pickupDao())
        locationId = runBlocking { insertLocation() }
    }

    /**
     * A pickup LOGGED just now but set 4 days earlier must land in its
     * actualPickupTime bucket (weekday 4 days back), never in the loggedAt
     * bucket — those weekdays differ by construction.
     */
    @Test
    fun backdatedPickupLandsInActualTimeBucket() = runTest {
        val actual = daysAgo(4, hour = 15)
        val logged = System.currentTimeMillis()
        db.pickupDao().insert(PickupEntity(locationId = locationId, customerAlias = "backdated", note = null, loggedAt = logged, actualPickupTime = actual))

        val matrix = heat.matrixFor(locationId).first()
        assertEquals("one row -> one bucket", 1, matrix.buckets.size)
        val bucket = matrix.buckets.single()
        assertEquals(expectedWeekday(actual), bucket.weekday)
        assertEquals(15, bucket.hour)
        assertEquals(1, bucket.count)
        // The loggedAt cell must not exist: 4 days back is a different weekday.
        val loggedCell = matrix.buckets.firstOrNull {
            it.weekday == expectedWeekday(logged) && it.hour == expectedHour(logged)
        }
        assertNull(loggedCell)
    }

    /** Rows scattered across distinct local weekday+hours bucket into distinct cells. */
    @Test
    fun bucketsByWeekdayAndHour() = runTest {
        val t1 = daysAgo(3, hour = 16)
        val t2 = daysAgo(5, hour = 8)
        db.pickupDao().insert(PickupEntity(locationId = locationId, customerAlias = "a", note = null, loggedAt = t1, actualPickupTime = t1))
        db.pickupDao().insert(PickupEntity(locationId = locationId, customerAlias = "b", note = null, loggedAt = t2, actualPickupTime = t2))
        db.pickupDao().insert(PickupEntity(locationId = locationId, customerAlias = "b2", note = null, loggedAt = t2, actualPickupTime = t2))

        val matrix = heat.matrixFor(locationId).first()
        assertEquals(2, matrix.buckets.size)
        assertEquals(2, matrix.buckets.first { it.weekday == expectedWeekday(t2) && it.hour == 8 }.count)
        assertEquals(1, matrix.buckets.first { it.weekday == expectedWeekday(t1) && it.hour == 16 }.count)
        assertEquals(3, matrix.totalCount)
        // Top slots rank the 2-count cell first.
        assertEquals(2, matrix.topSlots().first().count)
    }

    /** Gate boundary: 14 total is insufficient at gate 15; 15 total is shown. */
    @Test
    fun gateBoundary14Vs15() = runTest {
        val t = daysAgo(7, hour = 10)
        repeat(14) { i ->
            db.pickupDao().insert(PickupEntity(locationId = locationId, customerAlias = "row $i", note = null, loggedAt = t + i, actualPickupTime = t + i))
        }
        val fourteen = heat.matrixFor(locationId).first()
        assertTrue("14 of 15 must be insufficient", HeatRepositoryTest.insufficient(fourteen, minHeatCount = 15))

        db.pickupDao().insert(PickupEntity(locationId = locationId, customerAlias = "row 14", note = null, loggedAt = t, actualPickupTime = t))
        val fifteen = heat.matrixFor(locationId).first()
        assertEquals(15, fifteen.totalCount)
        assertTrue("15 of 15 must be shown", !HeatRepositoryTest.insufficient(fifteen, minHeatCount = 15))
    }

    /** Rows older than the 8-week window are excluded from buckets and total. */
    @Test
    fun trailingWindowExcludesOldRows() = runTest {
        val inside = daysAgo(7, hour = 10)
        val outside = daysAgo(60, hour = 10) // 60 days > 56-day window
        db.pickupDao().insert(PickupEntity(locationId = locationId, customerAlias = "new", note = null, loggedAt = inside, actualPickupTime = inside))
        db.pickupDao().insert(PickupEntity(locationId = locationId, customerAlias = "old", note = null, loggedAt = outside, actualPickupTime = outside))

        val matrix = heat.matrixFor(locationId).first()
        assertEquals(1, matrix.totalCount)
        assertEquals(listOf(1), matrix.buckets.map { it.count })
    }

    private suspend fun insertLocation(): Long =
        db.locationDao().insert(
            LocationEntity(
                source = LocationSource.MANUAL.value,
                placeId = null,
                name = "test-spot",
                category = "test",
                lat = 0.0,
                lng = 0.0,
                createdAt = 0L,
            )
        )

    /** now minus [days], with the clock set to [hour]:00 local — always inside the 8-week window. */
    private fun daysAgo(days: Int, hour: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, -days)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** Same (0=Mon..6=Sun) weekday Calendar gives java.time — the expected side of the SQL CASE. */
    private fun expectedWeekday(millis: Long): Int =
        (Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7

    private fun expectedHour(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

    companion object {
        /** The client-side gate check the UI applies: total < gate = insufficient. */
        fun insufficient(matrix: HeatMatrix, minHeatCount: Int): Boolean = matrix.totalCount < minHeatCount
    }
}
