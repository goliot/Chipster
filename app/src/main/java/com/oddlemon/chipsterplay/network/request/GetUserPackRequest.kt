package com.oddlemon.chipsterplay.network.request

data class GetUserPackRequest(
    val packId: Int,
    val userId: Int
)