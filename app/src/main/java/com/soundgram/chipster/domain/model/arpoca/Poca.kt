package com.soundgram.chipster.domain.model.arpoca

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Poca(
    @Json(name = "comment_count")
    val comment_count: Int,
    @Json(name = "cur_qty")
    val cur_qty: Int,
    @Json(name = "get_count")
    val get_count: Int,
    @Json(name = "id")
    val id: Int,
    @Json(name = "location_id")
    val location_id: Int?,
    @Json(name = "max_qty")
    val max_qty: Int?,
    @Json(name = "pack_id")
    val pack_id: Int,
    @Json(name = "poca_category_id")
    val poca_category_id: Int,
    @Json(name = "poca_img")
    val poca_img: String,
    @Json(name = "poca_level")
    val poca_level: Int,
    @Json(name = "poca_number")
    val poca_number: Int,
    @Json(name = "register_time")
    val register_time: String,
    @Json(name = "update_time")
    val update_time: String,
    @Json(name = "use_type")
    val use_type: Int
)