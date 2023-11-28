package com.oddlemon.chipsterplay.view.qr

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oddlemon.chipsterplay.domain.repository.QrRepository
import com.oddlemon.chipsterplay.network.RestfulAdapter
import com.oddlemon.chipsterplay.network.request.PostUserPackRequest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QrViewModel : ViewModel() {

    private val repository = QrRepository(
        service = RestfulAdapter.chipsterService
    )

    fun postUserPack(userId: Int, packId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) =
        viewModelScope.launch {
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
                        onError(it)
                    }
                )
            }
        }
}