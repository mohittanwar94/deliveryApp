package com.ezymd.restaurantapp.delivery.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezymd.restaurantapp.delivery.EzymdApplication
import com.ezymd.restaurantapp.delivery.login.model.LoginModel
import com.ezymd.restaurantapp.delivery.utils.BaseRequest
import com.ezymd.restaurantapp.delivery.utils.ErrorResponse
import com.ezymd.restaurantapp.network.ResultWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class LoginViewModel : ViewModel() {
    private var errorRequest: MutableLiveData<String> = MutableLiveData()
    private var loginRepository: LoginRepository? = null
    val loginResponse: MutableLiveData<LoginModel>
    val isLoading: MutableLiveData<Boolean>

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }


    fun startLoading(isLoadingValue: Boolean) {
        isLoading.value = isLoadingValue
    }

    init {
        loginRepository = LoginRepository.instance
        isLoading = MutableLiveData()
        loginResponse = MutableLiveData()
    }


    private fun showNetworkError() {
        errorRequest.postValue(EzymdApplication.getInstance().networkErrorMessage)
    }

    fun showError() = errorRequest

    private fun showGenericError(error: ErrorResponse?) {
        errorRequest.postValue(error?.message)
    }


    fun login(it: BaseRequest) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = loginRepository!!.login(
                it,
                Dispatchers.IO
            )
            isLoading.postValue(false)
            when (result) {
                is ResultWrapper.NetworkError -> showNetworkError()
                is ResultWrapper.GenericError -> showGenericError(result.error)
                is ResultWrapper.Success -> loginResponse.postValue(result.value)
            }
        }

    }

}


