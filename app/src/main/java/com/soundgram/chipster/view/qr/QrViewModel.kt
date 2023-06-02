package com.soundgram.chipster.view.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundgram.chipster.domain.model.PackInfo
import com.soundgram.chipster.domain.repository.QrRepository
import com.soundgram.chipster.network.RestfulAdapter
import com.soundgram.chipster.network.response.PostUserPackResponse
import kotlinx.coroutines.launch

class QrViewModel : ViewModel() {

    val repository = QrRepository(
        service = RestfulAdapter.chipsterService
    )

    fun getPocaInfo(userId: Int, packId: Int, onSuccess: () -> Unit) =
        viewModelScope.launch {
            repository.postUserPackResponse(
                postUserPackResponse = PostUserPackResponse(
                    packId = packId,
                    userId = userId
                )
            ).collect {
                it.handleResponse(
                    onSuccess = { packInfo ->
                        onSuccess()
                    },
                    onError = { error ->
                    }
                )
            }
        }
}