package com.soundgram.chipster.network.request

data class GetArPocaRequest(
    val packId: Int,
    val pocaId: Int,
    val userId: Int
)