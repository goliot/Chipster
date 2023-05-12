package com.soundgram.chipster.network.response

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.soundgram.chipster.domain.model.arpoca.Category
import com.soundgram.chipster.domain.model.arpoca.Location
import com.soundgram.chipster.domain.model.arpoca.Pack
import com.soundgram.chipster.domain.model.arpoca.Poca
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class GetTotalDataWithPackResponse(
    @SerializedName("categories")
    val categories: List<Category> = emptyList(),
    @SerializedName("locations")
    val locations: List<Location> = emptyList(),
    @SerializedName("packs")
    val packs: List<Pack> = emptyList(),
    @SerializedName("pocas")
    val pocas: List<Poca> = emptyList(),
    @SerializedName("response_code")
    val response_code: Int
)