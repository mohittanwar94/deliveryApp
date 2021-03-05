package com.ezymd.restaurantapp.delivery.login

import com.ezymd.restaurantapp.delivery.login.model.LoginModel
import com.ezymd.restaurantapp.delivery.network.NetworkCommonRequest
import com.ezymd.restaurantapp.delivery.utils.BaseRequest
import com.ezymd.restaurantapp.delivery.utils.SnapLog
import com.ezymd.restaurantapp.network.ApiClient
import com.ezymd.restaurantapp.network.ResultWrapper
import com.ezymd.restaurantapp.network.WebServices
import kotlinx.coroutines.CoroutineDispatcher


class LoginRepository private constructor() {


    suspend fun login(
        otp: BaseRequest,
        dispatcher: CoroutineDispatcher
    ): ResultWrapper<LoginModel> {

        SnapLog.print("Login repositry=====")
        val apiServices = ApiClient.client!!.create(WebServices::class.java)

        return NetworkCommonRequest.instance!!.safeApiCall(dispatcher) {
            apiServices.loginUser(
                otp.paramsMap
            )
        }


    }


    companion object {
        @Volatile
        private var sportsFeeRepository: LoginRepository? = null

        @JvmStatic
        val instance: LoginRepository?
            get() {
                if (sportsFeeRepository == null) {
                    synchronized(LoginRepository::class.java) {
                        sportsFeeRepository = LoginRepository()
                    }
                }
                return sportsFeeRepository
            }
    }

    init {
        if (sportsFeeRepository != null) {
            throw RuntimeException("Use getInstance() method to get the single instance of this class.")
        }
    }
}
