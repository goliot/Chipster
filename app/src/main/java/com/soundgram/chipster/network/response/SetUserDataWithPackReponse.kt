package com.soundgram.chipster.network.response

import com.soundgram.chipster.domain.model.arpoca.PocaInfo
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SetUserDataWithPackReponse(
    @Json(name = "poca_info")
    val poca_info: PocaInfo,
    @Json(name = "response_code")
    val response_code: Int
)