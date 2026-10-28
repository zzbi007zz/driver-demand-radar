package com.february10.driverradar.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * JVM tests for [PickupDao], run against a Room in-memory DB under
 * Robolectric.
 *
 * Room enables FK constraints by default, so every pickup test first creates
 * the parent [LocationEntity] through [LocationDao].
 *
 * (No "distinct aliases" query exists in the DAO as of Phase 01; the
 * history-ordered list covers the alias data.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PickupDaoTest {

    private lateinit var db: DemandRadarDb
    private lateinit var locationDao: LocationDao
    private lateinit var pickupDao: PickupDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = DemandRadarDb.inMemory(context)
        locationDao = db.locationDao()
        pickupDao = db.pickupDao()
    }

    @Test
    fun insertRoundTrips() = runTest {
        val locationId = createLocation("Pickup spot")
        val pickup = PickupEntity(
            locationId = locationId,
            customerAlias = "ACME rep",
            note = "meet at side door",
            loggedAt = 500L,
            actualPickupTime = 480L,
        )
        val id = pickupDao.insert(pickup)

        val rows = pickupDao.historyByLocation(locationId).first()
        val row = rows.single()
        assertNotNull(row.id)
        assertEquals("ACME rep", row.customerAlias)
        assertEquals("meet at side door", row.note)
        assertEquals(480L, row.actualPickupTime)
        assertTrue("insert must return the persisted row id", id >= 1L)
    }

    @Test
    fun historyByLocationOrdersNewestPickupFirst() = runTest {
        val a = createLocation("A")
        val b = createLocation("B")
        pickupDao.insert(pickup(a, "early", 100L, 100L))
        pickupDao.insert(pickup(a, "latest", 300L, 300L))
        pickupDao.insert(pickup(a, "middle", 200L, 200L))
        // A pickup at a different location must not leak into the query.
        pickupDao.insert(pickup(b, "other", 200L, 200L))

        val history = pickupDao.historyByLocation(a).first()
        assertEquals(3, history.size)
        assertEquals(
            listOf("latest", "middle", "early"),
            history.map { it.customerAlias },
        )
        assertEquals(listOf(300L, 200L, 100L), history.map { it.actualPickupTime })
    }

    @Test
    fun foreignKeyRejectsPickupWithoutParentLocation() = runTest {
        try {
            pickupDao.insert(pickup(locationId = 999_999L, "ghost", 0L, 0L))
            fail("expected SQLite FK constraint violation")
        } catch (expected: RuntimeException) {
            assertTrue(
                "expected a constraint error, got: ${expected.message}",
                expected.message.orEmpty().contains("constraint", ignoreCase = true),
            )
        }
    }

    private suspend fun createLocation(name: String): Long =
        locationDao.insert(
            LocationEntity(
                source = LocationSource.MANUAL.value,
                placeId = null,
                name = name,
                category = "test",
                lat = 0.0,
                lng = 0.0,
                createdAt = 0L,
            )
        )

    private fun pickup(locationId: Long, alias: String, loggedAt: Long, actual: Long) =
        PickupEntity(
            locationId = locationId,
            customerAlias = alias,
            note = null,
            loggedAt = loggedAt,
            actualPickupTime = actual,
        )
}
