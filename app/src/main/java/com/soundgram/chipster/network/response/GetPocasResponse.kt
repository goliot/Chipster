package com.soundgram.chipster.network.response

import com.soundgram.chipster.domain.model.PackInfo
import com.soundgram.chipster.domain.model.Poca

data class GetPocasResponse(
    val packInfo: PackInfo,
    val pocas: List<Poca>
)