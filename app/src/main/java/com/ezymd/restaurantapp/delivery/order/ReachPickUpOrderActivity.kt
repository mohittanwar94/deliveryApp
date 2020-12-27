package com.ezymd.restaurantapp.delivery.order

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ezymd.restaurantapp.delivery.BaseActivity
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.order.model.OrderModel
import com.ezymd.restaurantapp.delivery.order.model.OrderStatus
import com.ezymd.restaurantapp.delivery.tracker.TrackerViewModel
import com.ezymd.restaurantapp.delivery.utils.*
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.ncorti.slidetoact.SlideToActView
import kotlinx.android.synthetic.main.header_new.*
import kotlinx.android.synthetic.main.resturant_pick_up.*
import kotlinx.android.synthetic.main.user_live_tracking.*
import java.util.*
import kotlin.collections.ArrayList


class ReachPickUpOrderActivity : BaseActivity(), OnMapReadyCallback {
    val PATTERN_DASH_LENGTH_PX = 30
    val PATTERN_GAP_LENGTH_PX = 20
    val DASH: PatternItem = Dash(PATTERN_DASH_LENGTH_PX.toFloat())
    val GAP: PatternItem = Gap(PATTERN_GAP_LENGTH_PX.toFloat())
    val PATTERN_POLYGON_ALPHA: List<PatternItem> = Arrays.asList(GAP, DASH)

    private lateinit var defaultLocation: LatLng
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var grayPolyline: Polyline? = null
    private var blackPolyline: Polyline? = null
    val pointsList = ArrayList<LatLng>()


    private val orderModel by lazy { intent.getSerializableExtra(JSONKeys.OBJECT) as OrderModel }
    private var mMap: GoogleMap? = null
    private val trackViewModel by lazy { ViewModelProvider(this).get(TrackerViewModel::class.java) }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.right_in, R.anim.right_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.resturant_pick_up)
        setGUI()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)


    }

    @SuppressLint("SetTextI18n")
    private fun setGUI() {
        name.text = orderModel.restaurantName
        address.text = orderModel.restaurantAddress
        accept.text = getString(R.string.reached_for_pickup)
        headertext.text = getString(R.string.incoming_order)
        headertext.visibility = View.VISIBLE
        val slideListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                acceptOrder()
            }

        }
        accept.typeFace = Typeface.BOLD
        accept.onSlideCompleteListener = slideListener

        leftIcon.setOnClickListener {
            UIUtil.clickHandled(it)
            onBackPressed()
        }
        navigate.setOnClickListener {
            UIUtil.clickHandled(it)
            try {
                val gmmIntentUri =
                    Uri.parse("google.navigation:q=" + orderModel.restaurant_lat + "," + orderModel.restaurant_lang)
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun acceptOrder() {
        val baseRequest = BaseRequest(userInfo)
        baseRequest.paramsMap["order_id"] = "" + orderModel.orderId
        baseRequest.paramsMap["order_status"] = "" + OrderStatus.DELIVERY_BOY_REACHED_AT_RESTAURANT
        trackViewModel.acceptOrder(baseRequest)
    }

    private fun setObserver() {

        trackViewModel.routeInfoResponse.observe(this, Observer {
            if (it != null) {
                grayPolyline?.remove()
                blackPolyline?.remove()
                pointsList.clear()
                generateRouteOnMap(it)
                if (pointsList.size > 0)
                    showPath(pointsList)
                showDefaultLocationOnMap(defaultLocation)
                // showMovingCab(pointsList)
            }
        })
        trackViewModel.acceptRequest.observe(this, Observer {
            if (it != null) {
                if (it.status != ErrorCodes.SUCCESS) {
                    showError(false, it.message, null)
                } else {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    startActivityForResult(
                        Intent(
                            this@ReachPickUpOrderActivity,
                            OrderPickupActivity::class.java
                        ).putExtra(
                            JSONKeys.OBJECT,
                            orderModel
                        ), JSONKeys.LOCATION_REQUEST
                    )

                    overridePendingTransition(R.anim.left_in, R.anim.left_out)
                    setResult(Activity.RESULT_OK)
                    this.finish()
                }

            }
        })

        trackViewModel.locationUpdate.observe(this, Observer {
            if (it != null) {
                getUpdateRoot(defaultLocation)
            }
        })

        trackViewModel.showError().observe(this, Observer {
            if (it != null)
                showError(false, it, null)
        })


        trackViewModel.isLoading.observe(this, Observer {
            progress.visibility = if (it) {
                View.VISIBLE
            } else {
                View.GONE
            }
        })


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == JSONKeys.LOCATION_REQUEST && resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun getUpdateRoot(defaultLocation: LatLng) {
        var lat = defaultLocation.latitude
        var lng = defaultLocation.longitude
        val source = LatLng(lat, lng)
        lat = orderModel.restaurant_lat.toDouble()
        lng = orderModel.restaurant_lang.toDouble()
        val destination = LatLng(lat, lng)

        val hashMap = trackViewModel.getDirectionsUrl(
            source,
            destination,
            getString(R.string.google_maps_key)
        )
        trackViewModel.downloadRoute(hashMap)
    }


    override fun onMapReady(map: GoogleMap) {
        mMap = map
        mMap!!.setMaxZoomPreference(16f)
        mMap!!.uiSettings.isMyLocationButtonEnabled = false
        requestLocationUpdates()
        setObserver()
    }


    private fun bitmapDescriptorFromVector(
        @DrawableRes vectorDrawableResourceId: Int
    ): BitmapDescriptor? {
        val vectorDrawable =
            ContextCompat.getDrawable(this, vectorDrawableResourceId) as VectorDrawable?

        val h = vectorDrawable!!.intrinsicHeight
        val w = vectorDrawable.intrinsicWidth

        vectorDrawable.setBounds(0, 0, w, h)

        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bm)

    }

    fun generateRouteOnMap(result: List<List<HashMap<String, String>>>) {
        SnapLog.print("generate route======")
        for (element in result) {
            val path: List<HashMap<String, String>> = element
            for (j in path.indices) {
                val point: HashMap<String, String> = path[j]
                val lat: Double = point.get("lat")!!.toDouble()
                val lng: Double = point.get("lng")!!.toDouble()
                val position = LatLng(lat, lng)
                pointsList.add(position)
            }
        }

    }


    private fun moveCamera(latLng: LatLng) {
        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun animateCamera(latLng: LatLng) {
        val cameraPosition = CameraPosition.Builder().target(latLng).zoom(15.5f).build()
        mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }



    private fun addOriginDestinationMarkerAndGet(isSource: Boolean, latLng: LatLng): Marker {
        val bitmapDescriptor =
            if (isSource) {
                MapUtils.getSourceBitmap(this,R.drawable.ic_delivery_man)
            } else {
                MapUtils.getDestinationBitmap(this, R.drawable.ic_dining)
            }

        return mMap!!.addMarker(
            MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor)
        )
    }

    private fun showDefaultLocationOnMap(latLng: LatLng) {
        moveCamera(latLng)
        animateCamera(latLng)
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest()
        request.interval = 120000
        request.fastestInterval = 60000
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val client = LocationServices.getFusedLocationProviderClient(this)
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    location?.let {
                        val latlang = LatLng(location.latitude, location.longitude)
                        defaultLocation = latlang
                        getUpdateRoot(defaultLocation)
                    }

                }
            }, null)
        }
    }


    /**
     * This function is used to draw the path between the Origin and Destination.
     */
    private fun showPath(latLngList: ArrayList<LatLng>) {
        val builder = LatLngBounds.Builder()
        for (latLng in latLngList) {
            builder.include(latLng)
        }
        val bounds = builder.build()
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 2))

        val polylineOptions = PolylineOptions()
        polylineOptions.color(Color.GRAY)
        polylineOptions.width(0f)
        polylineOptions.addAll(latLngList)
        polylineOptions.pattern(PATTERN_POLYGON_ALPHA)
        grayPolyline = mMap!!.addPolyline(polylineOptions)

        val blackPolylineOptions = PolylineOptions()
        blackPolylineOptions.color(ContextCompat.getColor(this, R.color.color_002366))
        blackPolylineOptions.width(8f)
        blackPolylineOptions.pattern(PATTERN_POLYGON_ALPHA)
        blackPolyline = mMap!!.addPolyline(blackPolylineOptions)

        originMarker = addOriginDestinationMarkerAndGet(true, latLngList[0])
        //originMarker?.setAnchor(0.5f, 0.5f)
        originMarker?.isDraggable = false
        destinationMarker = addOriginDestinationMarkerAndGet(false, latLngList[latLngList.size - 1])
        //destinationMarker?.setAnchor(0.5f, 0.5f)
        destinationMarker?.isDraggable = false

        val polylineAnimator = AnimationUtils.polylineAnimator()
        polylineAnimator.addUpdateListener { valueAnimator ->
            val percentValue = (valueAnimator.animatedValue as Int)
            val index = (grayPolyline?.points!!.size) * (percentValue / 100.0f).toInt()
            blackPolyline?.points = grayPolyline?.points!!.subList(0, index)
        }
        polylineAnimator.start()
    }

}