package com.see.proxi.proxisee.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "point_of_interest")
data class PointOfInterest(
        @PrimaryKey
        var majorNumber: Int? = null,
        var minorNumber: Int? = null,
        var beaconRange: Int? = null,
        var text: String? = null,
        var specialDirectionMax: List<LatLong>? = null,
        var specialDirectionMin: List<LatLong>? = null,
        var specialDirectionText: List<String>? = null,
        var specialDirectionCode: List<Int>? = null
)