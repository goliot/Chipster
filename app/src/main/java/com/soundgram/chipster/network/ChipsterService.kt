package com.soundgram.chipster.network

import com.soundgram.chipster.domain.model.PackInfo
import com.soundgram.chipster.network.request.GetArPocaRequest
import com.soundgram.chipster.network.response.DefaultResponse
import com.soundgram.chipster.network.response.GetPocasResponse
import com.soundgram.chipster.network.request.PostUserPackRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChipsterService {
    @GET("api/v1/arpocasWithPackInfo/{packId}")
    suspend fun getPocasWithPackID(@Path("packId") packId: Int): Response<GetPocasResponse>

    @GET("api/v1/arpocasWithPackInfo/{packId}")
    fun getPocasWithPackIDTest(@Path("packId") packId: Int): Call<GetPocasResponse>

    @GET("api/v1/arpoca/pack/{packId}")
    suspend fun getPackInfo(@Path("packId") packId: Int): Response<PackInfo>

    @POST("api/v1/arpoca/userPack")
    suspend fun postUserPackResponse(
        @Body postUserPackRequest: PostUserPackRequest
    ): Response<DefaultResponse>

    @POST("phapi/arpoca/createPocaGet.php")
    suspend fun getArpocaGet(
        @Body getArPocaRequest: GetArPocaRequest
    ): Response<DefaultResponse>

}