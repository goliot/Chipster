package com.soundgram.chipster.network.request

import com.google.gson.annotations.SerializedName

data class GetArPocaRequest(
    @SerializedName("pack_id")
    val packId: Int,
    @SerializedName("poca_id")
    val pocaId: Int,
    @SerializedName("user_id")
    val userId: Int
)