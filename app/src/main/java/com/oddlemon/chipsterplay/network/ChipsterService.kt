package com.oddlemon.chipsterplay.network

import com.oddlemon.chipsterplay.domain.model.PackInfo
import com.oddlemon.chipsterplay.network.request.GetArPocaRequest
import com.oddlemon.chipsterplay.network.response.DefaultResponse
import com.oddlemon.chipsterplay.network.response.GetPocasResponse
import com.oddlemon.chipsterplay.network.request.PostUserPackRequest
import com.oddlemon.chipsterplay.network.response.GetPocaLocationResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ChipsterService {
    //@GET("api/v1/arpocasWithPackInfo/{packId}")
    @GET("phapi/arpoca/findPocaWithPackAndLocByPackId.php")
    suspend fun getPocasWithPackID(@Query("pack_id") pack_id: Int): Response<GetPocasResponse>

    @GET("phapi/arpoca/findLocationById.php")
    suspend fun findLocationByLocationId(@Query("location_id") locationId: Int): Response<GetPocaLocationResponse>

    //@GET("api/v1/arpocasWithPackInfo/{packId}")
    @GET("phapi/arpoca/findPocaWithPackAndLocByPackId.php")
    fun getPocasWithPackIDTest(@Query("pack_id") packId: Int): Call<GetPocasResponse>

    //@GET("api/v1/arpoca/pack/{packId}")
    @GET("phapi/arpoca/findPackById.php")
    suspend fun getPackInfo(@Query("pack_id") packId: Int): Response<PackInfo>

    //@POST("api/v1/arpoca/userPack")
    @POST("phapi/arpoca/createUserPack.php")
    suspend fun postUserPackResponse(
        @Body postUserPackRequest: PostUserPackRequest
    ): Response<DefaultResponse>

    @POST("phapi/arpoca/createPocaGet.php")
    suspend fun getArpocaGet(
        @Body getArPocaRequest: GetArPocaRequest
    ): Response<DefaultResponse>

}