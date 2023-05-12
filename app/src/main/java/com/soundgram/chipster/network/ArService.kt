package com.soundgram.chipster.network

import com.soundgram.chipster.network.response.GetTotalDataWithPackResponse
import com.soundgram.chipster.network.response.SetUserDataWithPackReponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ArService {

    @FormUrlEncoded
    @POST("arpoca_getTotalDataWithPack.php")
    suspend fun getTotalDataWithPack(
        @Field("packID") packID: Int,
    ): Response<GetTotalDataWithPackResponse>


    @FormUrlEncoded
    @POST("arpoca_setUserDataWithPack.php")
    suspend fun setUserDataWithPack(
        @Field("packID") packID: Int,
        @Field("userID") userID: Int,
        @Field("pocaID") pocaID: Int,
        @Field("locationID") locationID: Int,
    ): Response<SetUserDataWithPackReponse>


    @FormUrlEncoded
    @POST("arpoca_getTotalCheckInData.php")
    suspend fun getTotalCheckInData(
        @Field("totID") totId: Int,
        @Field("userID") userId: Int
    ): Response<GetTotalDataWithPackResponse>
}