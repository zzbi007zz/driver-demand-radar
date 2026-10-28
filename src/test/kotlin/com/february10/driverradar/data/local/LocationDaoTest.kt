package com.february10.driverradar.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.february10.driverradar.repo.LocationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * JVM tests for [LocationDao] plus the [LocationRepository.upsert] dedupe
 * path, run against a Room in-memory DB under Robolectric.
 *
 * Dedupe invariant under test: two saves sharing a non-null [placeId] collapse
 * to a single row whose id and createdAt stay stable, and the second save
 * updates the row in place.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocationDaoTest {

    private lateinit var db: DemandRadarDb
    private lateinit var dao: LocationDao
    private lateinit var repo: LocationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = DemandRadarDb.inMemory(context)
        dao = db.locationDao()
        repo = LocationRepository(dao)
    }

    @Test
    fun upsertDedupesByPlaceId() = runTest {
        val first = LocationEntity(
            source = LocationSource.GOOGLE_PLACE.value,
            placeId = "G-1",
            name = "Original name",
            category = "shop",
            lat = 1.0,
            lng = 2.0,
            createdAt = 100L,
        )
        val idFromFirst = repo.upsert(first)

        // Same placeId, different payload -> must update in place, not insert.
        val second = first.copy(name = "Renamed", category = "cafe", lat = 9.0, lng = 9.0, createdAt = 200L)
        val idFromSecond = repo.upsert(second)

        assertEquals("same placeId must map to the same stable id", idFromFirst, idFromSecond)

        val rows = dao.newest(100).first()
        assertEquals("dedupe keeps exactly one row per placeId", 1, rows.size)
        val row = rows.single()
        assertEquals(idFromFirst, row.id)
        assertEquals("Renamed", row.name)
        assertEquals("cafe", row.category)
        assertEquals(9.0, row.lat, 0.0)
        // upsert intentionally preserves the original createdAt, not the copy's.
        assertEquals(100L, row.createdAt)
    }

    @Test
    fun differentPlaceIdsStaySeparateRows() = runTest {
        repo.upsert(location(placeId = "A", name = "Place A"))
        repo.upsert(location(placeId = "B", name = "Place B"))

        val rows = dao.newest(100).first()
        assertEquals(2, rows.size)
        assertEquals(setOf("A", "B"), rows.map { it.placeId }.toSet())
    }

    @Test
    fun manualInsertRoundTrips() = runTest {
        val manual = location(placeId = null, name = "Home pin", category = "home", lat = 37.78, lng = -122.4)
        val id = dao.insert(manual)

        val rows = dao.newest(100).first()
        val row = rows.single { it.id == id }
        assertEquals("Home pin", row.name)
        assertEquals("home", row.category)
        assertEquals(37.78, row.lat, 0.0)
        assertEquals(-122.4, row.lng, 0.0)
        assertNull("manual locations carry no placeId", row.placeId)
        assertEquals(LocationSource.MANUAL.value, row.source)
        assertEquals(manual.createdAt, row.createdAt)
    }

    /** Convenience factory for a well-formed [LocationEntity]. */
    private fun location(
        placeId: String?,
        name: String,
        category: String = "cat",
        lat: Double = 1.0,
        lng: Double = 2.0,
    ) = LocationEntity(
        source = if (placeId != null) LocationSource.GOOGLE_PLACE.value else LocationSource.MANUAL.value,
        placeId = placeId,
        name = name,
        category = category,
        lat = lat,
        lng = lng,
        createdAt = 0L,
    )
}
