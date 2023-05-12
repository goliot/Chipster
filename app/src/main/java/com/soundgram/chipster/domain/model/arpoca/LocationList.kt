package com.soundgram.chipster.domain.model.arpoca

import com.soundgram.chipster.util.distanceOf

class LocationList(private val locationList: List<Location>) : List<Location> by locationList {

    fun getMinDistance(userLat: Double, userLong: Double) = locationList.minOfOrNull { location ->
        distanceOf(
            location.latitude,
            location.longitude,
            userLat,
            userLong,
            "meter"
        )
    }

    fun find(id: Int?) = locationList.find { location ->
        location.id == id
    }?.address ?: "주소를 못 찾았어요."
}