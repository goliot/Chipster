package com.soundgram.chipster.domain.repository

import com.soundgram.chipster.network.response.GetArucoMarkerResponse
import com.soundgram.chipster.network.ApiResult
import com.soundgram.chipster.network.MarkerService
import com.soundgram.chipster.network.safeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

class MarkerPlayerRepository(
    val service: MarkerService,
) {

    fun getMarkerIndex(
        onLoading: () -> Unit,
        onCompletion: () -> Unit,
        markerIndex: Int
    ): Flow<ApiResult<GetArucoMarkerResponse>> = safeFlow {
        service.getObContributors("1", "0", markerIndex.toString())
    }.onStart { onLoading() }.onCompletion { onCompletion() }

}