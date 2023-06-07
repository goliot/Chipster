package com.soundgram.chipster.network.request

data class GetUserPackRequest(
    val packId: Int,
    val userId: Int
)