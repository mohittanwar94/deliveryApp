package com.ezymd.restaurantapp.delivery.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ezymd.restaurantapp.delivery.BaseActivity
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.location.model.LocationModel
import com.ezymd.restaurantapp.delivery.tracker.TrackerViewModel
import com.ezymd.restaurantapp.delivery.utils.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.android.synthetic.main.tracker_activity.*


class MainFragment : Fragment(), OnMapReadyCallback {


    private var mMap: GoogleMap? = null
    private val trackViewModel by lazy { ViewModelProvider(requireActivity()).get(TrackerViewModel::class.java) }
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val userInfo by lazy { UserInfo.getInstance(requireActivity()) }
    private var mTimerIsRunning = false

    private var isNullViewRoot = false
    private var viewRoot: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isNullViewRoot = false
        if (viewRoot == null) {
            viewRoot = inflater.inflate(R.layout.fragment_main, container, false)
            isNullViewRoot = true
        }
        return viewRoot
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isNullViewRoot) {


            val mapFragment = childFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
            mapFragment!!.getMapAsync(this)
            setObserver()


        }
    }


    private fun setObserver() {

        trackViewModel.showError().observe(viewLifecycleOwner, Observer {
            if (it != null) {
                (activity as BaseActivity).showError(false, it, null)

            }
        })

        trackViewModel.isLoading().observe(viewLifecycleOwner, Observer {
            if (it != null) {
                progress.visibility = if (it) View.VISIBLE else View.GONE
            }
        })
        trackViewModel.errorRequest.observe(viewLifecycleOwner, Observer {
            if (it != null)
                (activity as BaseActivity).showError(false, it, null)
        })

        trackViewModel.acceptRequest.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                if (it.status != ErrorCodes.SUCCESS) {
                    (activity as BaseActivity).showError(false, it.message, null)
                }

            }
        })


    }

    private fun changeStatus() {
        val baseRequest = BaseRequest(userInfo)
        /*    baseRequest.paramsMap["order_id"] = "" + orderModel.orderId
            baseRequest.paramsMap["order_status"] = "" + OrderStatus.ORDER_ACCEPT_DELIVERY_BOY
            baseRequest.paramsMap["firebase_path"] =
                FireBaseConstants.path + userInfo!!.userID + "/" + orderModel.key
        */    trackViewModel.acceptOrder(baseRequest)
    }


    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        mMap = map
        mMap?.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                requireActivity(), R.raw.style_json
            )
        )
        mMap!!.isMyLocationEnabled = true
        mMap!!.setMaxZoomPreference(20f)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val isGranted = (activity as BaseActivity).checkLocationPermissions(object :
            BaseActivity.PermissionListener {
            override fun result(isGranted: Boolean) {
                setReadyLocation(mMap!!)
            }
        })
        if (isGranted) {
            setReadyLocation(mMap!!)
        }


    }

    @SuppressLint("MissingPermission")
    private fun setReadyLocation(googleMap: GoogleMap) {
        googleMap.isTrafficEnabled = false
        googleMap.isIndoorEnabled = false
        googleMap.isBuildingsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.setAllGesturesEnabled(true)
        googleMap.setOnCameraMoveStartedListener(GoogleMap.OnCameraMoveStartedListener {
            mTimerIsRunning = true
        })

        googleMap.setOnCameraIdleListener(GoogleMap.OnCameraIdleListener { // Cleaning all the markers.
            googleMap.clear()
            if (mTimerIsRunning) {
                val center = googleMap.cameraPosition.target
                val zoom = googleMap.cameraPosition.zoom
                setStartLocation(center.latitude, center.longitude, "", zoom)
                mTimerIsRunning = false
            }

        })
        getCurrentLocation()

    }


    override fun onResume() {
        super.onResume()
        SnapLog.print("" + mMap?.isMyLocationEnabled)

    }

    val REQUEST_CHECK_SETTINGS = 43
    private fun getCurrentLocation() {

        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = (10 * 1000).toLong()
        locationRequest.fastestInterval = 300

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val locationSettingsRequest = builder.build()

        val result =
            LocationServices.getSettingsClient(requireActivity())
                .checkLocationSettings(locationSettingsRequest)
        result.addOnCompleteListener { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                if (response!!.locationSettingsStates.isLocationPresent) {
                    getLastLocation()
                }
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val resolvable = exception as ResolvableApiException
                        resolvable.startResolutionForResult(
                            requireActivity(),
                            REQUEST_CHECK_SETTINGS
                        )
                    } catch (e: IntentSender.SendIntentException) {
                    } catch (e: ClassCastException) {
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    }
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationProviderClient.lastLocation
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful && task.result != null) {
                    val mLastLocation = task.result

                    val latLong = LatLng(mLastLocation.latitude, mLastLocation.longitude)
                    SnapLog.print("mLastLocation.latitude" + mLastLocation.latitude)
                    SnapLog.print("mLastLocation.longitude" + mLastLocation.longitude)
                    val cameraPosition = CameraPosition.Builder()
                        .target(latLong).zoom(17f).build()

                    mMap!!.animateCamera(
                        CameraUpdateFactory
                            .newCameraPosition(cameraPosition)
                    )


                } else {
                    Toast.makeText(
                        requireActivity(),
                        "No current location found",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }


    private fun setStartLocation(lat: Double, lng: Double, addr: String, zoom: Float) {

        val latLong = LatLng(lat, lng)

        val cameraPosition = CameraPosition.Builder()
            .target(latLong).zoom(zoom).build()

        mMap!!.animateCamera(
            CameraUpdateFactory
                .newCameraPosition(cameraPosition)
        )


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data!!.hasExtra(JSONKeys.LOCATION_OBJECT)) {
                        val location =
                            data.getParcelableExtra<LocationModel>(JSONKeys.LOCATION_OBJECT)
                        setStartLocation(location!!.lat, location.lang, "", 17f)
                    } else {
                        getCurrentLocation()
                    }
                }
            }
        }

    }


    override fun onPause() {
        super.onPause()

    }

    override fun onDestroy() {
        super.onDestroy()
    }


}