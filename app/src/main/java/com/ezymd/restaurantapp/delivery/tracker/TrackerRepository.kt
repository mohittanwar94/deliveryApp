package com.ezymd.restaurantapp.delivery.tracker

import com.ezymd.restaurantapp.delivery.network.NetworkCommonRequest
import com.ezymd.restaurantapp.delivery.order.model.OrderAcceptResponse
import com.ezymd.restaurantapp.delivery.utils.BaseRequest
import com.ezymd.restaurantapp.delivery.utils.BaseResponse
import com.ezymd.restaurantapp.delivery.utils.SnapLog
import com.ezymd.restaurantapp.network.ApiClient
import com.ezymd.restaurantapp.network.ResultWrapper
import com.ezymd.restaurantapp.network.WebServices
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.ConcurrentHashMap


class TrackerRepository private constructor() {


    suspend fun updateCoordinates(
        baseRequest: BaseRequest,
        dispatcher: CoroutineDispatcher
    ): ResultWrapper<BaseResponse> {

        SnapLog.print("track repositry=====")
        val apiServices = ApiClient.client!!.create(WebServices::class.java)

        return NetworkCommonRequest.instance!!.safeApiCall(dispatcher) {
            apiServices.locationUpdates(
                baseRequest.paramsMap!!, baseRequest.accessToken
            )
        }


    }


    suspend fun downloadRouteInfo(
        url: ConcurrentHashMap<String, String>,
        dispatcher: CoroutineDispatcher
    ): ResultWrapper<JsonObject> {

        SnapLog.print("track repositry=====")
        val apiServices = ApiClient.client!!.create(WebServices::class.java)

        return NetworkCommonRequest.instance!!.safeApiCall(dispatcher) {
            apiServices.downloadRoute(
                url.get("origin")!!,
                url.get("sensor")!!,
                url.get("destination")!!,
                url.get("mode")!!,
                url.get("key")!!,
                url.get("waypoints")!!
            )
        }


    }

    suspend fun acceptOrder(
        baseRequest: BaseRequest,
        dispatcher: CoroutineDispatcher
    ): ResultWrapper<OrderAcceptResponse> {

        SnapLog.print("track repositry=====")
        val apiServices = ApiClient.client!!.create(WebServices::class.java)

        return NetworkCommonRequest.instance!!.safeApiCall(dispatcher) {
            apiServices.acceptOrder(
                baseRequest.paramsMap,baseRequest.accessToken
            )
        }


    }

    companion object {
        @Volatile
        private var sportsFeeRepository: TrackerRepository? = null

        @JvmStatic
        val instance: TrackerRepository?
            get() {
                if (sportsFeeRepository == null) {
                    synchronized(TrackerRepository::class.java) {
                        sportsFeeRepository = TrackerRepository()
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
