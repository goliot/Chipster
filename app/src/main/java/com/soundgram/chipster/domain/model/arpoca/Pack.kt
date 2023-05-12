package com.soundgram.chipster.domain.model.arpoca

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Pack(
    @Json(name = "album_id")
    val album_id: Int,
    @Json(name = "end_time")
    val end_time: String?,
    @Json(name = "id")
    val id: Int,
    @Json(name = "level1msg")
    val level1msg: String,
    @Json(name = "level2msg")
    val level2msg: String,
    @Json(name = "level3msg")
    val level3msg: String,
    @Json(name = "level4msg")
    val level4msg: String,
    @Json(name = "level5msg")
    val level5msg: String,
    @Json(name = "pack_cardmotion_img")
    val pack_cardmotion_img: String,
    @Json(name = "pack_getmotion_img")
    val pack_getmotion_img: String,
    @Json(name = "pack_img")
    val packImg: String,
    @Json(name = "pack_name")
    val packName: String,
    @Json(name = "pack_pin_done_img")
    val pack_pin_done_img: String,
    @Json(name = "pack_pin_img")
    val pack_pin_img: String?,
    @Json(name = "pack_status")
    val pack_status: Int,
    @Json(name = "pack_target_img")
    val pack_target_img: String?,
    @Json(name = "pack_card_img")
    val pack_card_img: String,
    @Json(name = "pack_type")
    val pack_type: Int,
    @Json(name = "start_time")
    val start_time: String?,
    @Json(name = "timestamp")
    val timestamp: String,
    @Json(name = "tot_type")
    val tot_type: Int
)