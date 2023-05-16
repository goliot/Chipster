package com.soundgram.chipster.domain.repository

import com.soundgram.chipster.domain.model.PackInfo
import com.soundgram.chipster.network.ApiResult
import com.soundgram.chipster.network.ChipsterService
import com.soundgram.chipster.network.response.GetPocasResponse
import com.soundgram.chipster.network.safeFlow
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

}