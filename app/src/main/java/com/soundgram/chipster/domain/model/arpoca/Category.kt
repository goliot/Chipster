package com.soundgram.chipster.domain.model.arpoca


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Category(
    @Json(name = "id")
    val id: Int?,
    @Json(name = "pack_id")
    val pack_id: Int?,
    @Json(name = "poca_category_img")
    val poca_category_img: String?,
    @Json(name = "poca_category_name")
    val poca_category_name: String,
    @Json(name = "timestamp")
    val timestamp: String?
)