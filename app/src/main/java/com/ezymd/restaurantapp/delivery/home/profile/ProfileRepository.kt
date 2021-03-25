package com.ezymd.restaurantapp.delivery.home.profile

import com.ezymd.restaurantapp.delivery.network.NetworkCommonRequest
import com.ezymd.restaurantapp.delivery.order.model.OrderAcceptResponse
import com.ezymd.restaurantapp.delivery.utils.BaseRequest
import com.ezymd.restaurantapp.delivery.utils.SnapLog
import com.ezymd.restaurantapp.network.ApiClient
import com.ezymd.restaurantapp.network.ResultWrapper
import com.ezymd.restaurantapp.network.WebServices
import kotlinx.coroutines.CoroutineDispatcher

class ProfileRepository {


    suspend fun logout(
        baseRequest: BaseRequest,
        dispatcher: CoroutineDispatcher
    ): ResultWrapper<LogoutModel> {

        val apiServices = ApiClient.client!!.create(WebServices::class.java)

        return NetworkCommonRequest.instance!!.safeApiCall(dispatcher) {
            apiServices.logout(
                baseRequest.accessToken
            )
        }


    }
    suspend fun changeDutyStatus(
        baseRequest: BaseRequest,
        dispatcher: CoroutineDispatcher
    ): ResultWrapper<OrderAcceptResponse> {

        SnapLog.print("track repositry=====")
        val apiServices = ApiClient.client!!.create(WebServices::class.java)

        return NetworkCommonRequest.instance!!.safeApiCall(dispatcher) {
            apiServices.changeDutyStatus(
                baseRequest.paramsMap,baseRequest.accessToken
            )
        }


    }


    companion object {
        @Volatile
        private var sportsFeeRepository: ProfileRepository? = null

        @JvmStatic
        val instance: ProfileRepository?
            get() {
                if (sportsFeeRepository == null) {
                    synchronized(ProfileRepository::class.java) {
                        sportsFeeRepository = ProfileRepository()
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