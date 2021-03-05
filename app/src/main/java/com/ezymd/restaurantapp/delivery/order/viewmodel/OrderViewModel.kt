package com.ezymd.restaurantapp.delivery.order.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezymd.restaurantapp.delivery.EzymdApplication
import com.ezymd.restaurantapp.delivery.order.model.OrderAcceptResponse
import com.ezymd.restaurantapp.delivery.order.model.OrderBaseModel
import com.ezymd.restaurantapp.delivery.utils.BaseRequest
import com.ezymd.restaurantapp.delivery.utils.ErrorResponse
import com.ezymd.restaurantapp.delivery.utils.SingleLiveEvent
import com.ezymd.restaurantapp.network.ResultWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

class OrderViewModel @ViewModelInject constructor(
    private val loginRepository: OrderListRepository
    //private val networkHelper: NetworkHelper
) : ViewModel() {

    var errorRequest: SingleLiveEvent<String>
    val baseResponse: MutableLiveData<OrderBaseModel>
    val cancelResponse: MutableLiveData<OrderBaseModel>
    val processingResponse: MutableLiveData<OrderBaseModel>
    val assignResponse: MutableLiveData<OrderAcceptResponse>
    val isLoading: MutableLiveData<Boolean>

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }


    init {

        // loginRepository = OrderListRepository.instance
        isLoading = MutableLiveData()
        baseResponse = MutableLiveData()
        errorRequest = SingleLiveEvent()
        assignResponse = MutableLiveData()
        cancelResponse = MutableLiveData()
        processingResponse = MutableLiveData()


    }


    private fun showNetworkError() {
        errorRequest.postValue(EzymdApplication.getInstance().networkErrorMessage)
    }


    private fun showGenericError(error: ErrorResponse?) {
        errorRequest.postValue(error?.message)
    }


    fun orderList(baseRequest: BaseRequest) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = loginRepository!!.listOrders(
                baseRequest,
                Dispatchers.IO
            )
            isLoading.postValue(false)
            when (result) {
                is ResultWrapper.NetworkError -> showNetworkError()
                is ResultWrapper.GenericError -> showGenericError(result.error)
                is ResultWrapper.Success -> {
                    baseResponse.postValue(result.value)

                }
            }
        }

    }

    fun processingOrderList(baseRequest: BaseRequest) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = loginRepository!!.listOrders(
                baseRequest,
                Dispatchers.IO
            )
            isLoading.postValue(false)
            when (result) {
                is ResultWrapper.NetworkError -> showNetworkError()
                is ResultWrapper.GenericError -> showGenericError(result.error)
                is ResultWrapper.Success -> {
                    processingResponse.postValue(result.value)

                }
            }
        }

    }


    fun cancelOrder(baseRequest: BaseRequest) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = loginRepository!!.cancelOrders(
                baseRequest,
                Dispatchers.IO
            )
            isLoading.postValue(false)
            when (result) {
                is ResultWrapper.NetworkError -> showNetworkError()
                is ResultWrapper.GenericError -> showGenericError(result.error)
                is ResultWrapper.Success -> {
                    cancelResponse.postValue(result.value)

                }
            }
        }

    }


    fun assignOrder(baseRequest: BaseRequest) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = loginRepository!!.assignOrder(
                baseRequest,
                Dispatchers.IO
            )
            isLoading.postValue(false)
            when (result) {
                is ResultWrapper.NetworkError -> showNetworkError()
                is ResultWrapper.GenericError -> showGenericError(result.error)
                is ResultWrapper.Success -> {
                    assignResponse.postValue(result.value)

                }
            }
        }

    }

}