package com.ezymd.restaurantapp.delivery.network

import com.ezymd.restaurantapp.delivery.utils.ErrorResponse
import com.ezymd.restaurantapp.network.ResultWrapper
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class NetworkComonRequest {


    suspend fun <T> safeApiCall(
        dispatcher: CoroutineDispatcher,
        apiCall: suspend () -> T
    ): ResultWrapper<T> {
        return withContext(dispatcher) {
            try {
                ResultWrapper.Success(apiCall.invoke())
            } catch (throwable: Throwable) {
                when (throwable) {
                    is IOException -> ResultWrapper.NetworkError
                    is HttpException -> {
                        val code = throwable.code()
                        val errorResponse = convertErrorBody(throwable)
                        ResultWrapper.GenericError(code, errorResponse)
                    }
                    else -> {
                        ResultWrapper.GenericError(null, null)
                    }
                }
            }
        }
    }

    private fun convertErrorBody(throwable: HttpException): ErrorResponse? {
        return try {
            throwable.response()?.errorBody()?.let {
                val gsonAdapter = Gson().getAdapter(ErrorResponse::class.java)
                gsonAdapter.fromJson(it.charStream())


            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            null
        }
    }


}