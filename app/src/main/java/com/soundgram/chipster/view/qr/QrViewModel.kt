package com.soundgram.chipster.view.qr

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundgram.chipster.domain.repository.QrRepository
import com.soundgram.chipster.network.RestfulAdapter
import com.soundgram.chipster.network.request.PostUserPackRequest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QrViewModel : ViewModel() {

    private val repository = QrRepository(
        service = RestfulAdapter.chipsterService
    )

    fun postUserPack(userId: Int, packId: Int, onSuccess: () -> Unit, onError: () -> Unit) =
        viewModelScope.launch {
            Log.i("dlgocks1", "postUserPack In")
            repository.postUserPackResponse(
                postUserPackRequest = PostUserPackRequest(
                    packId = packId,
                    userId = userId
                )
            ).collectLatest { result ->
                Log.i("dlgocks1", "postUserPack: $result")
                result.handleResponse(
                    onSuccess = {
                        onSuccess()
                    },
                    onError = {
                        onError()
                    }
                )
            }
        }
}