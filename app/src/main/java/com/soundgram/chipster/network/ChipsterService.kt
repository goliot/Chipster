package com.soundgram.chipster.network

import com.soundgram.chipster.domain.model.PackInfo
import com.soundgram.chipster.network.response.GetPocasResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ChipsterService {
    @GET("v1/arpocas-with-packInfo/{packId}")
    suspend fun getPocasWithPackID(@Path("packId") packId: Int): Response<GetPocasResponse>

    @GET("v1/arpoca/pack/{packId}")
    suspend fun getPackInfo(@Path("packId") packId: Int): Response<PackInfo>

}