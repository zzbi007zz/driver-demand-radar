package com.february10.driverradar.repo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.february10.driverradar.data.local.DemandRadarDb
import com.february10.driverradar.data.local.LocationEntity
import com.february10.driverradar.data.local.LocationSource
import com.february10.driverradar.data.local.PickupEntity
import com.february10.driverradar.model.ExportBackup
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
 * JSON export round-trip: serialize both tables, parse back, assert row
 * counts + field fidelity. Exercises [ExportRepository.toJson] / [parse]
 * against a real in-memory Room DB (serialization is Android-free).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExportRepositoryTest {

    private lateinit var db: DemandRadarDb
    private lateinit var repo: ExportRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = DemandRadarDb.inMemory(context)
        repo = ExportRepository(context, db.locationDao(), db.pickupDao())
    }

    @Test
    fun roundTripPreservesRowCountsAndFields() = runTest {
        val loc = db.locationDao().insert(
            LocationEntity(
                source = LocationSource.GOOGLE_PLACE.value,
                placeId = "ChIJ-some-google-id",
                name = "Airport",
                category = "school",
                lat = 1.5,
                lng = 2.5,
                createdAt = 1000L,
            )
        )
        db.locationDao().insert(
            LocationEntity(
                source = LocationSource.MANUAL.value,
                placeId = null,
                name = "Manual pin",
                category = "manual",
                lat = 3.0,
                lng = 4.0,
                createdAt = 2000L,
            )
        )
        db.pickupDao().insert(PickupEntity(locationId = loc, customerAlias = "ACME rep", note = "side door", loggedAt = 500L, actualPickupTime = 480L))
        db.pickupDao().insert(PickupEntity(locationId = loc, customerAlias = "Beta", note = null, loggedAt = 600L, actualPickupTime = 550L))

        val text = repo.toJson(repo.buildBackup())
        val back = repo.parse(text)

        assertEquals("location rows survive the round trip", 2, back.locations.size)
        assertEquals("pickup rows survive the round trip", 2, back.pickups.size)
        assertTrue("exportedAt is stamped", back.exportedAt > 0L)

        val airport = back.locations.single { it.name == "Airport" }
        assertEquals("ChIJ-some-google-id", airport.placeId)
        assertEquals("school", airport.category)
        assertEquals(1.5, airport.lat, 0.0)
        assertEquals(loc, airport.id)

        val manual = back.locations.single { it.name == "Manual pin" }
        assertNull("manual placeId must stay null", manual.placeId)

        val acme = back.pickups.single { it.customerAlias == "ACME rep" }
        assertEquals(loc, acme.locationId)
        assertEquals("side door", acme.note)
        assertEquals(480L, acme.actualPickupTime)
        val beta = back.pickups.single { it.customerAlias == "Beta" }
        assertNull("empty note must stay null", beta.note)
    }

    @Test
    fun emptyDatabaseExportsEmptyLists() = runTest {
        val back = repo.parse(repo.toJson(repo.buildBackup()))
        assertTrue(back.locations.isEmpty())
        assertTrue(back.pickups.isEmpty())
        assertTrue(back.exportedAt > 0L)
    }

    @Test
    fun toJsonIsPrettyPrinted() = runTest {
        val text = repo.toJson(ExportBackup(emptyList(), emptyList(), 42L))
        // Pretty-print inserts indentation; the serialized output spans lines.
        assertTrue("expected multi-line pretty JSON", text.lines().size > 1)
        assertTrue("explicit nulls keep the pickup field present", text.contains("\"pickups\""))
    }
}
