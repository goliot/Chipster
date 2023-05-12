package com.soundgram.chipster.domain.model.arpoca

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PocaInfo(
    val comment_count: String,
    val cur_qty: String,
    val get_count: String,
    val id: String,
    val location_id: String,
    val pack_id: String,
    val poca_id: String,
    val poca_img: String,
    val poca_number: String,
    val register_time: String,
    val update_time: String,
    val use_type: String,
    val user_id: String
)

