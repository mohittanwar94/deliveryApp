package com.ezymd.restaurantapp.delivery.order

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ezymd.restaurantapp.delivery.BaseActivity
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.customviews.SnapTextView
import com.ezymd.restaurantapp.delivery.order.model.OrderModel
import com.ezymd.restaurantapp.delivery.tracker.TrackerViewModel
import com.ezymd.restaurantapp.delivery.utils.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.header_new.*
import kotlinx.android.synthetic.main.order_completed_details_with_customer.*


class OrderCompletedActivity : BaseActivity(), OnMapReadyCallback {
    private lateinit var defaultLocation: LatLng
    private var grayPolyline: Polyline? = null
    private var blackPolyline: Polyline? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    val pointsList = ArrayList<LatLng>()
    private val orderModel by lazy { intent.getSerializableExtra(JSONKeys.OBJECT) as OrderModel }
    private var showingDetails = false
    private var mMap: GoogleMap? = null
    private val trackViewModel by lazy { ViewModelProvider(this).get(TrackerViewModel::class.java) }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.right_in, R.anim.right_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.order_completed)
        setGUI()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)


    }

    private fun setItems(itemInfo: SnapTextView) {
        val builder = StringBuilder("")
        for (item in orderModel.orderItems) {
            builder.append(item.item)
            builder.append(" x ")
            builder.append(item.qty)
            builder.append("\n")
        }

        if (orderModel.deliveryInstruction != "") {
            builder.append("\n")
            builder.append(getString(R.string.delivery_instruction) + " - " + orderModel.deliveryInstruction)
        }

        itemInfo.text = builder.toString()
    }


    @SuppressLint("SetTextI18n")
    private fun setGUI() {
        name.text = orderModel.restaurantName
        address.text = orderModel.restaurantAddress

        userName.text = orderModel.username
        userAddress.text = orderModel.address
        setItems(items)
        headertext.text = getString(R.string.order_completed)
        headertext.visibility = View.VISIBLE


        orderStatusSub.text =
            getString(R.string.order_has_been_delivered) + " " + orderModel.username + "."
        leftIcon.setOnClickListener {
            UIUtil.clickHandled(it)
            onBackPressed()
        }

        setOrderDetails()

    }

    private fun setOrderDetails() {
        val spannable = SpannableString(getString(R.string.show_order_details))
        spannable.setSpan(UnderlineSpan(), 0, spannable.length, 0)
        showDetails.text = spannable
        showDetails.setOnClickListener {
            UIUtil.clickHandled(it)
            showingDetails = !showingDetails
            if (showingDetails) {
                items.visibility = View.VISIBLE
                val spannable = SpannableString(getString(R.string.hide_details))
                spannable.setSpan(
                    UnderlineSpan(),
                    0,
                    spannable.length,
                    0
                )
                showDetails.text = spannable
            } else {
                items.visibility = View.GONE
                val spannable = SpannableString(getString(R.string.show_order_details))
                spannable.setSpan(
                    UnderlineSpan(),
                    0,
                    spannable.length,
                    0
                )
                showDetails.text = spannable
            }

        }
    }

    private fun setObserver() {

        var lat = orderModel.delivery_lat.toDouble()
        var lng = orderModel.delivery_lang.toDouble()
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

        trackViewModel.routeInfoResponse.observe(this, Observer {
            if (it != null) {
                grayPolyline?.remove()
                blackPolyline?.remove()
                pointsList.clear()
                generateRouteOnMap(it)
                if (pointsList.size > 0)
                    showPath(pointsList)


            }
        })

        trackViewModel.showError().observe(this, Observer {
            if (it != null)
                showError(false, it, null)
        })
        trackViewModel.errorRequest.observe(this, Observer {
            if (it != null)
                showError(false, it, null)
        })


    }


    override fun onMapReady(map: GoogleMap) {
        mMap = map
        mMap!!.setMaxZoomPreference(13f)
        defaultLocation =
            LatLng(orderModel.delivery_lat.toDouble(), orderModel.delivery_lang.toDouble())
        mMap!!.uiSettings.isMyLocationButtonEnabled = false
        showDefaultLocationOnMap(defaultLocation)
        setObserver()
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

    private fun addCarMarkerAndGet(latLng: LatLng): Marker {
        val bitmapDescriptor = MapUtils.getCarBitmap(this)
        return mMap!!.addMarker(
            MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor)
        )
    }

    private fun addOriginDestinationMarkerAndGet(isSource: Boolean, latLng: LatLng): Marker {
        val bitmapDescriptor =
            if (isSource) {
                MapUtils.getSourceBitmap(this, R.drawable.ic_user_location)
            } else {
                MapUtils.getDestinationBitmap(this, R.drawable.ic_dining_large)
            }

        return mMap!!.addMarker(
            MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor)
        )
    }

    private fun showDefaultLocationOnMap(latLng: LatLng) {
        moveCamera(latLng)
        animateCamera(latLng)
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
        polylineOptions.width(12f)
        polylineOptions.addAll(latLngList)
        grayPolyline = mMap!!.addPolyline(polylineOptions)

        val blackPolylineOptions = PolylineOptions()
        blackPolylineOptions.color(ContextCompat.getColor(this, R.color.color_002366))
        blackPolylineOptions.width(12f)
        blackPolyline = mMap!!.addPolyline(blackPolylineOptions)

        originMarker = addOriginDestinationMarkerAndGet(true, latLngList[0])
        originMarker?.isDraggable = false
        destinationMarker = addOriginDestinationMarkerAndGet(false, latLngList[latLngList.size - 1])
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