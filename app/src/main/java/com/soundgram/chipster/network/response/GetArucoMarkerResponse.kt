package com.soundgram.chipster.network.response

import com.soundgram.chipster.network.response.Marker
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetArucoMarkerResponse(
    @Json(name = "markers")
    val markers: List<Marker>,
    @Json(name = "response_code")
    val response_code: Int
)