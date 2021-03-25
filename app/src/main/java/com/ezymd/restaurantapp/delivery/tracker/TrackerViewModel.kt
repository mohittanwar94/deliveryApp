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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class TrackerViewModel : ViewModel() {
    var errorRequest: MutableLiveData<String>
    private var loginRepository: TrackerRepository? = null
    val routeInfoResponse: MutableLiveData<ArrayList<LatLng>>
    val firebaseResponse: MutableLiveData<DataSnapshot>
    val locationUpdate = MutableLiveData<BaseResponse>()
    val acceptRequest = MutableLiveData<OrderAcceptResponse>()
    val dutyStatus = MutableLiveData<Int>()
    val isLoading: MutableLiveData<Boolean>


    override fun onCleared() {

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


    /*fun startTimer(order_id: String, userInfo: UserInfo) {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                isRunUpdate.postValue(true)


            }


        }, 120000, 120000);
    }*/

    private fun showNetworkError() {
        errorRequest.postValue(EzymdApplication.getInstance().networkErrorMessage!!)
    }

    fun showError() = errorRequest

    fun showGenericError(error: ErrorResponse?) {
        errorRequest.postValue(error?.message)
    }


    fun getDirectionsUrl(
        origin: LatLng,
        wayPoints: LatLng,
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
        haspMap.put("waypoints", "via:" + wayPoints.latitude + "," + wayPoints.longitude)
        return haspMap
    }


    fun downloadLatestCoordinates(baseRequest: BaseRequest) {
        //  isLoading.postValue(true)
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
                    locationUpdate.postValue(result.value!!)
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
                    acceptRequest.postValue(result.value!!)
                }
            }
        }
    }


    fun changeDutyStatus(baseRequest: BaseRequest) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = loginRepository!!.changeDutyStatus(
                baseRequest,
                Dispatchers.IO
            )
            isLoading.postValue(false)
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
                        errorRequest.postValue(result.value.message)
                    }

                }
            }
        }

    }


    fun downloadRoute(url: ConcurrentHashMap<String, String>) {
        // isLoading.postValue(true)
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
                    withContext(Dispatchers.IO) {
                        routeInfoResponse.postValue(
                            parseResponse(result.value.toString())
                        )
                    }
                }
            }
        }

    }

    private fun parseResponse(value: String): ArrayList<LatLng> {
        val pointsList = ArrayList<LatLng>()
        val routes = ArrayList<List<HashMap<String, String>>>()
        try {
            val jsonObject = JSONObject(value)
            val parser = DirectionsJSONParser()
            routes.addAll(parser.parse(jsonObject))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        SnapLog.print("generate route======")
        for (element in routes) {
            val path: List<HashMap<String, String>> = element
            for (j in path.indices) {
                val point: HashMap<String, String> = path[j]
                val lat: Double = point.get("lat")!!.toDouble()
                val lng: Double = point.get("lng")!!.toDouble()
                val position = LatLng(lat, lng)
                pointsList.add(position)
            }

        }
        return pointsList


    }


}


