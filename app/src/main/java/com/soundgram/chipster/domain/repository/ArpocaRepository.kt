package com.soundgram.chipster.domain.repository

import com.soundgram.chipster.network.ApiResult
import com.soundgram.chipster.network.ChipsterService
import com.soundgram.chipster.network.request.GetArPocaRequest
import com.soundgram.chipster.network.response.DefaultResponse
import com.soundgram.chipster.network.response.GetPocasResponse
import com.soundgram.chipster.network.safeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import retrofit2.Retrofit

class ArpocaRepository(
    val service: ChipsterService,
) {
    fun getPocasWithPackId(
        packId: Int,
        onLoading: () -> Unit = {},
        onComplete: () -> Unit = {},
    ): Flow<ApiResult<GetPocasResponse>> = safeFlow {
        val retrofit: Retrofit = Retrofit.Builder().baseUrl("https://api.soundgram.ai/").build()
        val serviceTest = retrofit.create(ChipsterService::class.java)
        serviceTest.getPocasWithPackIDTest(2).enqueue()
        service.getPocasWithPackID(packId = packId)
    }.onStart { onLoading() }.onCompletion { onComplete() }


    fun getArpocaGet(
        getArPocaRequest: GetArPocaRequest
    ): Flow<ApiResult<DefaultResponse>> = safeFlow {
        service.getArpocaGet(
            getArPocaRequest
        )
    }
}