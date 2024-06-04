package com.oddlemon.chipsterplay.network.response

import com.oddlemon.chipsterplay.domain.model.Location
import com.oddlemon.chipsterplay.domain.model.PackInfo
import com.oddlemon.chipsterplay.domain.model.Poca

data class GetPocasResponse(
    val packInfo: PackInfo,
    val pocas: List<Poca>
)

data class GetPocaLocationResponse(
    val location: Location
)