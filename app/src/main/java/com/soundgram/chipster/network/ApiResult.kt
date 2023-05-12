package com.soundgram.chipster.network

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
        errorMsg: String = "인터넷 상태를 확인해주세요.",
        onError: (String) -> Unit,
        onSuccess: (T) -> Unit,
    ) {
        when (this@ApiResult) {
            is Success -> onSuccess(this@ApiResult.value)
            is Empty -> handleException {
                onError(emptyMsg)
            }
            is Error -> handleException(exception) {
                onError(errorMsg)
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

fun <T> safeFlow(apiFunc: suspend () -> Response<T>): Flow<ApiResult<T>> = flow {
    val res = apiFunc.invoke()
    try {
        if (res.isSuccessful) {
            val body = res.body() ?: throw java.lang.NullPointerException()
            emit(ApiResult.Success(body))
        }
    } catch (e: NullPointerException) {
        emit(ApiResult.Empty)
    } catch (e: HttpException) {
        emit(ApiResult.Error(exception = e))
    } catch (e: Exception) {
        emit(ApiResult.Error(exception = e))
    } finally {
    }
}



