package com.oddlemon.chipsterplay.network

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import retrofit2.Response

sealed class ApiResult<out T> {
    data class Success<out T>(val value: T) : ApiResult<T>()
    object Empty : ApiResult<Nothing>()
    data class Error(
        val exception: Throwable? = null,
        var message: String? = ""
    ) : ApiResult<Nothing>()

    fun handleResponse(
        emptyMsg: String = "결과 값이 없어요.",
        errorMsg: String = "네트워크 요청에 실패했어요.",
        onError: (String) -> Unit,
        onSuccess: (T) -> Unit,
    ) {
        when (this@ApiResult) {
            is Success -> onSuccess(this@ApiResult.value)
            is Empty -> handleException {
                onError(emptyMsg)
            }

            is Error -> handleException(exception) {
                onError(message ?: errorMsg)
            }
        }
    }

    private fun handleException(
        exception: Throwable? = null,
        onError: () -> Unit,
    ) {
        exception?.printStackTrace()
        onError()
    }
}

data class ErrorResponse(
    val code: Int,
    val message: String?
)

fun <T> safeFlow(apiFunc: suspend () -> Response<T>): Flow<ApiResult<T>> = flow {
    val res = apiFunc.invoke()
    val gson = Gson()
    try {
        if (res.isSuccessful) {
            val body = res.body()
            if (body == null) {
                val errorResponse = gson.fromJson(
                    res.errorBody()?.string(),
                    ErrorResponse::class.java
                )
                val errorMessage = errorResponse?.message ?: "Unknown error"
                emit(ApiResult.Error(message = errorMessage))
            } else {
                emit(ApiResult.Success(body))
            }
        } else {
            val errorResponse = gson.fromJson(
                res.errorBody()?.string(),
                ErrorResponse::class.java
            )
            val errorMessage = errorResponse?.message ?: "Unknown error"
            emit(ApiResult.Error(message = errorMessage))
        }
    } catch (e: Exception) {
        emit(ApiResult.Empty)
    }
}
