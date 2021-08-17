package com.ezymd.restaurantapp.delivery.order.viewmodel

import com.ezymd.restaurantapp.delivery.network.NetworkComonRequest
import com.ezymd.restaurantapp.delivery.order.model.OrderAcceptResponse
import com.ezymd.restaurantapp.delivery.order.model.OrderBaseModel
import com.ezymd.restaurantapp.delivery.utils.BaseRequest
import com.ezymd.restaurantapp.network.ResultWrapper
import com.ezymd.restaurantapp.network.WebServices
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class OrderListRepository @Inject constructor(
    private val apiHelper: WebServices,
    private val networkCommonRequest: NetworkComonRequest
) {


    suspend fun listOrders(
        baseRequest: BaseRequest,
        dispatcher: CoroutineDispatcher
    ): ResultWrapper<OrderBaseModel> {


        return networkCommonRequest.safeApiCall(dispatcher) {
            apiHelper.orderList(
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


        return networkCommonRequest.safeApiCall(dispatcher) {
            apiHelper.cancelOrderList(
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


        return networkCommonRequest.safeApiCall(dispatcher) {
            apiHelper.assignOrder(
                baseRequest.paramsMap.get("order_id")!!, baseRequest.accessToken
            )
        }


    }


}