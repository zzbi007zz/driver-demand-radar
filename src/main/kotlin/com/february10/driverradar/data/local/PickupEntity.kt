package com.february10.driverradar.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single logged pickup at a location.
 *
 * No PII by design: [customerAlias] is a caller-chosen label (e.g. "ACME
 * rep") and [note] is a free-text local-only note.
 */
@Entity(
    tableName = "pickups",
    // PK declared on the id field via @PrimaryKey(autoGenerate=true); Room 2.8
    // rejects a duplicate declaration through @Entity(primaryKeys=...).
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class, // Room 2.8 renamed this property from entityClass
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["locationId"])],
)
data class PickupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val locationId: Long,
    val customerAlias: String,
    val note: String?,
    /** When the pickup was logged (epoch millis). */
    val loggedAt: Long,
    /** When the pickup actually happened (epoch millis; <= loggedAt). */
    val actualPickupTime: Long,
)
