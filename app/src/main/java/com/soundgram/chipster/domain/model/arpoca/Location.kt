package com.soundgram.chipster.domain.model.arpoca

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Location(
    @Json(name = "address")
    val address: String,
    @Json(name = "id")
    val id: Int,
    @Json(name = "latitude")
    val latitude: Double,
    @Json(name = "longitude")
    val longitude: Double,
    @Json(name = "pack_id")
    val pack_id: Int,
    @Json(name = "poca_id")
    val poca_id: Int?,
    @Json(name = "register_time")
    val register_time: String
)