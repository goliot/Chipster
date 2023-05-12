package com.soundgram.chipster.domain.repository

import com.soundgram.chipster.network.response.GetTotalDataWithPackResponse
import com.soundgram.chipster.network.ApiResult
import com.soundgram.chipster.network.ArService
import com.soundgram.chipster.network.response.SetUserDataWithPackReponse
import com.soundgram.chipster.network.safeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

class ArpocaRepository(
    val service: ArService,
) {

    fun getTotalDataWithPack(
        pacKId: Int,
        onLoading: () -> Unit = {},
        onComplete: () -> Unit = {},
    ): Flow<ApiResult<GetTotalDataWithPackResponse>> = safeFlow {
        service.getTotalDataWithPack(packID = pacKId)
    }.onStart { onLoading() }.onCompletion { onComplete() }

    fun setUserDataWithPack(
        packId: Int,
        userId: Int,
        pocaId: Int,
        locationId: Int,
        onLoading: () -> Unit = {},
        onComplete: () -> Unit = {},
    ): Flow<ApiResult<SetUserDataWithPackReponse>> = safeFlow {
        service.setUserDataWithPack(
            packID = packId, userID = userId, pocaID = pocaId, locationID = locationId
        )
    }.onStart { onLoading() }.onCompletion { onComplete() }

    fun getTotalCheckInData(
        totId: Int,
        userId: Int,
        onLoading: () -> Unit = {},
        onComplete: () -> Unit = {},
    ): Flow<ApiResult<GetTotalDataWithPackResponse>> = safeFlow {
        service.getTotalCheckInData(totId = totId, userId = userId)
    }.onStart { onLoading() }.onCompletion { onComplete() }
}