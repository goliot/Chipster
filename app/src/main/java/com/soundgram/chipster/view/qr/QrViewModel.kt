package com.soundgram.chipster.view.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundgram.chipster.domain.model.PackInfo
import com.soundgram.chipster.domain.repository.QrRepository
import com.soundgram.chipster.network.RestfulAdapter
import kotlinx.coroutines.launch

class QrViewModel : ViewModel() {

    val repository = QrRepository(
        service = RestfulAdapter.chipsterService
    )

    fun getPocaInfo(packId: Int, onSuccess: (PackInfo) -> Unit) = viewModelScope.launch {
        repository.getPackInfo(packId = packId).collect {
            it.handleResponse(
                onSuccess = { packInfo ->
                    onSuccess(packInfo)
                },
                onError = { error ->
                }
            )
        }
    }
}