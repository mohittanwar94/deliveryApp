package com.ezymd.restaurantapp.delivery.home.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezymd.restaurantapp.delivery.EzymdApplication
import com.ezymd.restaurantapp.delivery.utils.BaseRequest
import com.ezymd.restaurantapp.delivery.utils.ErrorCodes
import com.ezymd.restaurantapp.delivery.utils.ErrorResponse
import com.ezymd.restaurantapp.delivery.utils.SnapLog
import com.ezymd.restaurantapp.network.ResultWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    var errorRequest: MutableLiveData<String>
    private var loginRepository: ProfileRepository? = null
    val mResturantData: MutableLiveData<LogoutModel>
    val isLoading: MutableLiveData<Boolean>
    val dutyStatus = MutableLiveData<Int>()

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }


    init {

        loginRepository = ProfileRepository.instance
        isLoading = MutableLiveData()
        mResturantData = MutableLiveData()
        errorRequest = MutableLiveData()


    }


    fun logout(baseRequest: BaseRequest) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = loginRepository!!.logout(
                baseRequest,
                Dispatchers.IO
            )
            isLoading.postValue(false)
            when (result) {
                is ResultWrapper.NetworkError -> showNetworkError()
                is ResultWrapper.GenericError -> showGenericError(result.error)
                is ResultWrapper.Success -> mResturantData.postValue(result.value!!)
            }

        }

    }


    private fun showNetworkError() {
        errorRequest.postValue(EzymdApplication.getInstance().networkErrorMessage!!)
    }


    private fun showGenericError(error: ErrorResponse?) {
        errorRequest.postValue(error?.message)
    }

    fun changeDutyStatus(baseRequest: BaseRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = loginRepository!!.changeDutyStatus(
                baseRequest,
                Dispatchers.IO
            )
            when (result) {
                is ResultWrapper.NetworkError -> showNetworkError()
                is ResultWrapper.GenericError -> showGenericError(result.error)
                is ResultWrapper.Success -> {
                    SnapLog.print(result.value.toString())
                    if (result.value.status == ErrorCodes.SUCCESS) {
                        val value = dutyStatus.value
                        dutyStatus.postValue(
                            if (value == 0) {
                                1
                            } else {
                                0
                            }
                        )
                    } else {
                        dutyStatus.postValue(dutyStatus.value)
                        errorRequest.postValue(result.value.message)
                    }

                }
            }
        }

    }

}