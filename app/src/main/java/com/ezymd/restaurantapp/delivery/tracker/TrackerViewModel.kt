package com.ezymd.restaurantapp.delivery.tracker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezymd.restaurantapp.delivery.EzymdApplication
import com.ezymd.restaurantapp.delivery.order.model.OrderAcceptResponse
import com.ezymd.restaurantapp.delivery.utils.*
import com.ezymd.restaurantapp.network.ResultWrapper
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class TrackerViewModel : ViewModel() {
    var errorRequest: MutableLiveData<String>
    private var loginRepository: TrackerRepository? = null
    val routeInfoResponse: MutableLiveData<ArrayList<List<HashMap<String, String>>>>
    val firebaseResponse: MutableLiveData<DataSnapshot>
    val locationUpdate = MutableLiveData<BaseResponse>()
    val acceptRequest = MutableLiveData<OrderAcceptResponse>()
    val isLoading: MutableLiveData<Boolean>
    val timer = Timer()

    override fun onCleared() {
        timer.cancel()
        super.onCleared()
        viewModelScope.cancel()
    }

    fun isLoading(): LiveData<Boolean?> {
        return isLoading
    }

    fun startLoading(isLoadingValue: Boolean) {
        isLoading.value = isLoadingValue
    }

    init {
        errorRequest = MutableLiveData()
        loginRepository = TrackerRepository.instance
        firebaseResponse = MutableLiveData()
        isLoading = MutableLiveData()
        routeInfoResponse = MutableLiveData()
    }


    fun startTimer(order_id: String, userInfo: UserInfo) {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val baseRequest = BaseRequest(userInfo)
                baseRequest.paramsMap["id"] = order_id
                downloadLatestCoordinates(baseRequest)

            }


        }, 120000, 120000);
    }

    private fun showNetworkError() {
        errorRequest.postValue(EzymdApplication.getInstance().networkErrorMessage)
    }

    fun showError() = errorRequest

    fun showGenericError(error: ErrorResponse?) {
        errorRequest.postValue(error?.message)
    }


    fun getDirectionsUrl(
        origin: LatLng,
        dest: LatLng,
        key: String
    ): ConcurrentHashMap<String, String> {
        val haspMap = ConcurrentHashMap<String, String>()
        val str_origin = "" + origin.latitude + "," + origin.longitude
        val str_dest = "" + dest.latitude + "," + dest.longitude


        haspMap.put("origin", str_origin)
        haspMap.put("destination", str_dest)
        haspMap.put("sensor", "false")
        haspMap.put("mode", "driving")
        haspMap.put("key", key)

        return haspMap
    }


    fun downloadLatestCoordinates(baseRequest: BaseRequest) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = loginRepository!!.updateCoordinates(
                baseRequest,
                Dispatchers.IO
            )
            isLoading.postValue(false)
            when (result) {
                is ResultWrapper.NetworkError -> showNetworkError()
                is ResultWrapper.GenericError -> showGenericError(result.error)
                is ResultWrapper.Success -> {
                    SnapLog.print(result.value.toString())
                    locationUpdate.postValue(result.value)
                }
            }
        }

    }

    fun acceptOrder(baseRequest: BaseRequest) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = loginRepository!!.acceptOrder(
                baseRequest,
                Dispatchers.IO
            )
            isLoading.postValue(false)
            when (result) {
                is ResultWrapper.NetworkError -> showNetworkError()
                is ResultWrapper.GenericError -> showGenericError(result.error)
                is ResultWrapper.Success -> {
                    SnapLog.print(result.value.toString())
                    acceptRequest.postValue(result.value)
                }
            }
        }

    }


    fun downloadRoute(url: ConcurrentHashMap<String, String>) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = loginRepository!!.downloadRouteInfo(
                url,
                Dispatchers.IO
            )
            isLoading.postValue(false)
            when (result) {
                is ResultWrapper.NetworkError -> showNetworkError()
                is ResultWrapper.GenericError -> showGenericError(result.error)
                is ResultWrapper.Success -> {
                    SnapLog.print(result.value.toString())
                    routeInfoResponse.postValue(parseResponse(result.value.toString()))
                }
            }
        }

    }

    private fun parseResponse(value: String): ArrayList<List<HashMap<String, String>>> {
        val routes = ArrayList<List<HashMap<String, String>>>()
        try {
            val jsonObject = JSONObject(value)
            val parser = DirectionsJSONParser()
            routes.addAll(parser.parse(jsonObject))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return routes


    }


}


