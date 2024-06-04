package com.oddlemon.chipsterplay.domain.repository

import com.oddlemon.chipsterplay.network.ApiResult
import com.oddlemon.chipsterplay.network.ChipsterService
import com.oddlemon.chipsterplay.network.request.GetArPocaRequest
import com.oddlemon.chipsterplay.network.response.DefaultResponse
import com.oddlemon.chipsterplay.network.response.GetPocaLocationResponse
import com.oddlemon.chipsterplay.network.response.GetPocasResponse
import com.oddlemon.chipsterplay.network.safeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

class ArpocaRepository(
    val service: ChipsterService,
) {
    fun getPocasWithPackId(
        packId: Int,
        onLoading: () -> Unit = {},
        onComplete: () -> Unit = {},
    ): Flow<ApiResult<GetPocasResponse>> = safeFlow {
//        serviceTest.getPocasWithPackIDTest(2).enqueue()
        service.getPocasWithPackID(packId)
    }.onStart { onLoading() }.onCompletion { onComplete() }

    fun findLocationByLocationId(
        locationId: Int,
        onLoading: () -> Unit = {},
        onComplete: () -> Unit = {},
    ): Flow<ApiResult<GetPocaLocationResponse>> = safeFlow {
        service.findLocationByLocationId(locationId)
    }.onStart { onLoading() }.onCompletion { onComplete() }

    fun getArpocaGet(
        getArPocaRequest: GetArPocaRequest
    ): Flow<ApiResult<DefaultResponse>> = safeFlow {
        service.getArpocaGet(
            getArPocaRequest
        )
    }
}