package com.oddlemon.chipsterplay.domain.repository

import com.oddlemon.chipsterplay.domain.model.PackInfo
import com.oddlemon.chipsterplay.network.ApiResult
import com.oddlemon.chipsterplay.network.ChipsterService
import com.oddlemon.chipsterplay.network.response.DefaultResponse
import com.oddlemon.chipsterplay.network.request.PostUserPackRequest
import com.oddlemon.chipsterplay.network.safeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

class QrRepository(
    val service: ChipsterService,
) {
    fun getPackInfo(
        packId: Int,
        onLoading: () -> Unit = {},
        onComplete: () -> Unit = {},
    ): Flow<ApiResult<PackInfo>> = safeFlow {
        service.getPackInfo(packId = packId)
    }.onStart { onLoading() }.onCompletion { onComplete() }

    fun postUserPackResponse(postUserPackRequest: PostUserPackRequest): Flow<ApiResult<DefaultResponse>> =
        safeFlow {
            service.postUserPackResponse(postUserPackRequest = postUserPackRequest)
        }


}