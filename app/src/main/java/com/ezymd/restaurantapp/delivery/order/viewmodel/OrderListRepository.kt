package com.ezymd.vendor.order

import com.ezymd.restaurantapp.delivery.order.model.OrderAcceptResponse
import com.ezymd.restaurantapp.delivery.order.model.OrderBaseModel
import com.ezymd.restaurantapp.delivery.utils.BaseRequest
import com.ezymd.restaurantapp.network.ApiClient
import com.ezymd.restaurantapp.network.NetworkCommonRequest
import com.ezymd.restaurantapp.network.ResultWrapper
import com.ezymd.restaurantapp.network.WebServices
import kotlinx.coroutines.CoroutineDispatcher

class OrderListRepository {


    suspend fun listOrders(
        baseRequest: BaseRequest,
        dispatcher: CoroutineDispatcher
    ): ResultWrapper<OrderBaseModel> {

        val apiServices = ApiClient.client!!.create(WebServices::class.java)

        return NetworkCommonRequest.instance!!.safeApiCall(dispatcher) {
            apiServices.orderList(
                baseRequest.paramsMap["device_token"]!!,
                baseRequest.paramsMap["device_id"]!!,
                baseRequest.paramsMap["order_status"]!!, baseRequest.accessToken
            )
        }


    }


    suspend fun cancelOrders(
        baseRequest: BaseRequest,
        dispatcher: CoroutineDispatcher
    ): ResultWrapper<OrderBaseModel> {

        val apiServices = ApiClient.client!!.create(WebServices::class.java)

        return NetworkCommonRequest.instance!!.safeApiCall(dispatcher) {
            apiServices.cancelOrderList(
                baseRequest.paramsMap["device_token"]!!,
                baseRequest.paramsMap["device_id"]!!,
                baseRequest.paramsMap["order_status"]!!,
                baseRequest.paramsMap["delivery_boy_id"]!!, baseRequest.accessToken
            )
        }


    }


    suspend fun assignOrder(
        baseRequest: BaseRequest,
        dispatcher: CoroutineDispatcher
    ): ResultWrapper<OrderAcceptResponse> {

        val apiServices = ApiClient.client!!.create(WebServices::class.java)

        return NetworkCommonRequest.instance!!.safeApiCall(dispatcher) {
            apiServices.assignOrder(
                baseRequest.paramsMap.get("order_id")!!, baseRequest.accessToken
            )
        }


    }


    companion object {
        @Volatile
        private var sportsFeeRepository: OrderListRepository? = null

        @JvmStatic
        val instance: OrderListRepository?
            get() {
                if (sportsFeeRepository == null) {
                    synchronized(OrderListRepository::class.java) {
                        sportsFeeRepository = OrderListRepository()
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