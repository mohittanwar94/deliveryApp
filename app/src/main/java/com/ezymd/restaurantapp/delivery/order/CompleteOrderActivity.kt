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
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ezymd.restaurantapp.delivery.BaseActivity
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.customviews.SnapTextView
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
import com.google.firebase.database.DataSnapshot
import com.ncorti.slidetoact.SlideToActView
import kotlinx.android.synthetic.main.complete_order.*
import kotlinx.android.synthetic.main.header_new.*
import kotlinx.android.synthetic.main.order_completed_details_with_customer.address
import kotlinx.android.synthetic.main.order_completed_details_with_customer.items
import kotlinx.android.synthetic.main.order_completed_details_with_customer.name
import kotlinx.android.synthetic.main.order_completed_details_with_customer.showDetails
import kotlinx.android.synthetic.main.order_details_with_customer.*


class CompleteOrderActivity : BaseActivity(), OnMapReadyCallback {
    private lateinit var defaultLocation: LatLng
    private var grayPolyline: Polyline? = null
    private var blackPolyline: Polyline? = null
    private var movingCabMarker: Marker? = null
    private var previousLatLng: LatLng? = null
    private var currentLatLng: LatLng? = null
    private val mMarkers: HashMap<String, Marker> = HashMap()
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null

    val pointsList = ArrayList<LatLng>()

    private var showingDetails = false
    private var mMap: GoogleMap? = null
    private val trackViewModel by lazy { ViewModelProvider(this).get(TrackerViewModel::class.java) }


    private val orderModel by lazy { intent.getSerializableExtra(JSONKeys.OBJECT) as OrderModel }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.right_in, R.anim.right_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.complete_order)
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
        accept.text = getString(R.string.complete_the_order)
        headertext.text = getString(R.string.deliver_order)
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

        setOrderDetails()
        navigate.setOnClickListener {
            UIUtil.clickHandled(it)
            try {
                val gmmIntentUri =
                    Uri.parse("google.navigation:q=" + orderModel.delivery_lat + "," + orderModel.delivery_lang)
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        userCall.setOnClickListener {
            UIUtil.clickAlpha(it)
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:" + orderModel.phoneNo)
            startActivity(intent)

        }


        restCall.setOnClickListener {
            UIUtil.clickAlpha(it)
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:" + orderModel.restPhoneNO)
            startActivity(intent)

        }
    }

    private fun acceptOrder() {
        val baseRequest = BaseRequest(userInfo)
        baseRequest.paramsMap["order_id"] = "" + orderModel.orderId
        baseRequest.paramsMap["order_status"] = "" + OrderStatus.ORDER_COMPLETED
        trackViewModel.acceptOrder(baseRequest)

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
        //if (item.orderPickupStatus == OrderStatus.ORDER_ASSIGN_FOR_DELIVERY)
        //  trackViewModel.startTimer(orderModel.orderId.toString(), userInfo!!)

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

        trackViewModel.locationUpdate.observe(this, Observer {
            if (it != null) {
                getUpdateRoot()
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



        trackViewModel.acceptRequest.observe(this, Observer {
            if (it != null) {
                if (it.status != ErrorCodes.SUCCESS) {
                    showError(false, it.message, null)
                } else {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_OK)
                    this.finish()
                }

            }
        })


    }

    private fun getUpdateRoot() {
        var lat = orderModel.restaurant_lat.toDouble()
        var lng = orderModel.restaurant_lang.toDouble()
        val source = LatLng(lat, lng)
        lat = orderModel.delivery_lat.toDouble()
        lng = orderModel.delivery_lang.toDouble()
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
        defaultLocation =
            LatLng(orderModel.delivery_lat.toDouble(), orderModel.delivery_lang.toDouble())
        mMap!!.uiSettings.isMyLocationButtonEnabled = false
        showDefaultLocationOnMap(defaultLocation)
        setObserver()
        requestLocationUpdates()
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
                        val baseRequest = BaseRequest(userInfo)
                        baseRequest.paramsMap["id"] = "" + orderModel.orderId
                        baseRequest.paramsMap["lat"] = "" + location.latitude
                        baseRequest.paramsMap["lang"] = "" + location.longitude

                        trackViewModel.downloadLatestCoordinates(baseRequest)
                        updateCarLocation(latlang)

                    }

                }
            }, null)
        }
    }

    private fun setMarker(dataSnapshot: DataSnapshot) {

        val key = dataSnapshot.key
        val value = dataSnapshot.value as HashMap<*, *>
        val lat = (value["latitude"].toString()).toDouble()
        val lng = (value["longitude"].toString()).toDouble()
        val location = LatLng(lat, lng)
        if (!mMarkers.containsKey(key)) {
            mMarkers[key!!] = mMap!!.addMarker(
                MarkerOptions().title("Your Order")
                    .icon(bitmapDescriptorFromVector(R.drawable.ic_delivery_man))
                    .position(location)
            )

        } else {
            mMarkers[key]?.setPosition(location)
        }
        val builder = LatLngBounds.Builder()
        for (marker in mMarkers.values) {
            builder.include(marker.position)
        }
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300))

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

    private fun addCarMarkerAndGet(latLng: LatLng): Marker {
        val bitmapDescriptor = MapUtils.getCarBitmap(this)
        return mMap!!.addMarker(
            MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor)
        )
    }

    private fun addOriginDestinationMarkerAndGet(isSource: Boolean, latLng: LatLng): Marker {
        val bitmapDescriptor =
            if (isSource) {
                MapUtils.getSourceBitmap(this,R.drawable.ic_dining)
            } else {
                MapUtils.getDestinationBitmap(this,R.drawable.ic_user_location)
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

        originMarker = addOriginDestinationMarkerAndGet(false, latLngList[0])
        originMarker?.isDraggable = false
        destinationMarker = addOriginDestinationMarkerAndGet(true, latLngList[latLngList.size - 1])
        destinationMarker?.isDraggable = false

        val polylineAnimator = AnimationUtils.polylineAnimator()
        polylineAnimator.addUpdateListener { valueAnimator ->
            val percentValue = (valueAnimator.animatedValue as Int)
            val index = (grayPolyline?.points!!.size) * (percentValue / 100.0f).toInt()
            blackPolyline?.points = grayPolyline?.points!!.subList(0, index)
        }
        polylineAnimator.start()
    }

    /**
     * This function is used to update the location of the Cab while moving from Origin to Destination
     */
    private fun updateCarLocation(latLng: LatLng) {
        if (movingCabMarker == null) {
            movingCabMarker = addCarMarkerAndGet(latLng)
        }
        if (previousLatLng == null) {
            currentLatLng = latLng
            previousLatLng = currentLatLng
            movingCabMarker?.position = currentLatLng
            movingCabMarker?.setAnchor(0.5f, 0.5f)
            animateCamera(currentLatLng!!)
        } else {
            previousLatLng = currentLatLng
            currentLatLng = latLng
            val valueAnimator = AnimationUtils.carAnimator()
            valueAnimator.addUpdateListener { va ->
                if (currentLatLng != null && previousLatLng != null) {
                    val multiplier = va.animatedFraction
                    val nextLocation = LatLng(
                        multiplier * currentLatLng!!.latitude + (1 - multiplier) * previousLatLng!!.latitude,
                        multiplier * currentLatLng!!.longitude + (1 - multiplier) * previousLatLng!!.longitude
                    )
                    movingCabMarker?.position = nextLocation
                    val rotation = MapUtils.getRotation(previousLatLng!!, nextLocation)
                    if (!rotation.isNaN()) {
                        movingCabMarker?.rotation = rotation
                    }
                    movingCabMarker?.setAnchor(0.5f, 0.5f)
                    animateCamera(nextLocation)
                }
            }
            valueAnimator.start()
        }
    }


}