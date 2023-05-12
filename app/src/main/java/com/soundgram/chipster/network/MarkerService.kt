package com.soundgram.chipster.network

import com.soundgram.chipster.network.response.GetArucoMarkerResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface MarkerService {

    @FormUrlEncoded
    @POST("armarker_getIndexDataWithTotid.php")
    suspend fun getObContributors(
        @Field("totid") totid: String,
        @Field("albumID") albumID: String,
        @Field("markerIndex") markerIndex: String
    ): Response<GetArucoMarkerResponse>
}