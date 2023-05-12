package com.soundgram.chipster.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Marker(
    @Json(name = "address")
    val address: String,
    @Json(name = "album_id")
    val album_id: Int,
    @Json(name = "end_time")
    val end_time: String,
    @Json(name = "frame")
    val frame: Int,
    @Json(name = "id")
    val id: Int,
    @Json(name = "latitude")
    val latitude: Int,
    @Json(name = "loc_flag")
    val loc_flag: Int,
    @Json(name = "longitude")
    val longitude: Int,
    @Json(name = "marker_index")
    val marker_index: Int,
    @Json(name = "register_time")
    val register_time: String,
    @Json(name = "start_time")
    val start_time: String,
    @Json(name = "markers")
    val tot_id: Int,
    @Json(name = "update_time")
    val update_time: String,
    @Json(name = "use_type")
    val use_type: Int,
    @Json(name = "video_title")
    val video_title: String,
    @Json(name = "video_type")
    val video_type: Int,
    @Json(name = "video_url")
    val video_url: String
)