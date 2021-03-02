package com.ezymd.restaurantapp.delivery.order

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.VectorDrawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ezymd.restaurantapp.delivery.BaseActivity
import com.ezymd.restaurantapp.delivery.EzymdApplication
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.customviews.SnapTextView
import com.ezymd.restaurantapp.delivery.order.model.OrderModel
import com.ezymd.restaurantapp.delivery.order.model.OrderStatus
import com.ezymd.restaurantapp.delivery.tracker.TrackerService
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
import com.google.maps.android.PolyUtil
import com.ncorti.slidetoact.SlideToActView
import kotlinx.android.synthetic.main.complete_order.*
import kotlinx.android.synthetic.main.header_new.*
import kotlinx.android.synthetic.main.order_completed_details_with_customer.address
import kotlinx.android.synthetic.main.order_completed_details_with_customer.items
import kotlinx.android.synthetic.main.order_completed_details_with_customer.name
import kotlinx.android.synthetic.main.order_completed_details_with_customer.showDetails
import kotlinx.android.synthetic.main.order_details_with_customer.*


class CompleteOrderActivity : BaseActivity(), OnMapReadyCallback {
    private var start_rotation: Float=0f
    private lateinit var defaultLocation: LatLng
    private var grayPolyline: Polyline? = null
    private var blackPolyline: Polyline? = null
    private var movingCabMarker: Marker? = null
    private var previousLatLng: LatLng? = null
    private var currentLatLng: LatLng? = null
    private val mMarkers: HashMap<String, Marker> = HashMap()
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var isDrawFirst = false

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
                if (orderModel.paymentType == PaymentMethodTYPE.COD) {
                    showConfirmationDialog()
                } else {
                    acceptOrder()
                }
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

        if (orderModel.paymentType == PaymentMethodTYPE.COD) {
            cash.visibility = View.VISIBLE
            cash.text = "Cash to be collected " + getString(R.string.dollor) + orderModel.total
        }
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this, R.style.alert_dialog_theme)
        builder.setMessage("Have You Received Cash " + getString(R.string.dollor) + orderModel.total + "?")
            .setCancelable(false)
            .setPositiveButton("Yes", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, id: Int) {
                    dialog?.dismiss()
                    acceptOrder()
                }
            })
            .setNegativeButton("No", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    dialog.dismiss()
                    accept.resetSlider()

                }
            })
        val alert: AlertDialog = builder.create()
        alert.setTitle("Cash Collected?")
        alert.show()

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
        var lat = orderModel.restaurant_lat.toDouble()
        var lng = orderModel.restaurant_lang.toDouble()
        val source = LatLng(lat, lng)
        lat = orderModel.delivery_lat.toDouble()
        lng = orderModel.delivery_lang.toDouble()
        val destination = LatLng(lat, lng)

        val hashMap = trackViewModel.getDirectionsUrl(
            source,
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
                pointsList.addAll(it)
                if (pointsList.size > 0) {
                    if (!isDrawFirst) {
                        showPath(pointsList)
                    } else {
                        val polylineOptions = PolylineOptions()
                        polylineOptions.color(Color.GRAY)
                        polylineOptions.width(12f)
                        polylineOptions.addAll(pointsList)

                        grayPolyline = mMap!!.addPolyline(polylineOptions)
                        val polylineOptions1 = PolylineOptions()
                        polylineOptions1.color(Color.BLACK)
                        polylineOptions1.width(12f)
                        polylineOptions1.addAll(pointsList)
                        blackPolyline = mMap!!.addPolyline(polylineOptions1)


                    }
                }


            }
        })


        trackViewModel.locationUpdate.observe(this, Observer {
            if (it != null) {
                // getUpdateRoot()
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
                    EzymdApplication.getInstance().isRefresh.postValue(true)
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    startActivityForResult(
                        Intent(this, OrderCompletedActivity::class.java).putExtra(
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


        trackViewModel.isLoading.observe(this, Observer {
            progress.visibility = if (it) {
                View.VISIBLE
            } else {
                View.GONE
            }
        })

    }

    private fun getUpdateRoot(latlang: LatLng) {
        if (!PolyUtil.isLocationOnPath(latlang, pointsList, true, 50.0)) {
            var lat = orderModel.restaurant_lat.toDouble()
            var lng = orderModel.restaurant_lang.toDouble()
            val source = LatLng(lat, lng)
            lat = orderModel.delivery_lat.toDouble()
            lng = orderModel.delivery_lang.toDouble()
            val destination = LatLng(lat, lng)

            val hashMap = trackViewModel.getDirectionsUrl(
                source,
                latlang,
                destination,
                getString(R.string.google_maps_key)
            )
            trackViewModel.downloadRoute(hashMap)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        mMap = map
        mMap?.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                this, R.raw.style_json
            )
        );

        mMap!!.setMaxZoomPreference(25f)
        mMap!!.isTrafficEnabled = false
        mMap!!.isIndoorEnabled = false
        mMap!!.isBuildingsEnabled = true
        mMap!!.uiSettings.isCompassEnabled = false
        defaultLocation =
            LatLng(orderModel.restaurant_lat.toDouble(), orderModel.restaurant_lang.toDouble())
        mMap!!.uiSettings.isMyLocationButtonEnabled = false
        showDefaultLocationOnMap(defaultLocation)
        setObserver()
        requestLocationUpdates()
        startTrackerService()
    }

    private fun startTrackerService() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(
                Intent(this, TrackerService::class.java).putExtra(
                    JSONKeys.ID,
                    orderModel.orderId
                )
            )
        } else {
            startService(
                Intent(this, TrackerService::class.java).putExtra(
                    JSONKeys.ID,
                    orderModel.orderId
                )
            )
        }
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest()
        request.interval = 5000
        request.fastestInterval = 5000
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

                        // save on server


                        getUpdateRoot(latlang)
                        SnapLog.print("location.bearing====" + location.bearing)
                        if (previousLatLng == null) {
                            updateCarLocation(latlang, location.bearing, location.hasBearing())

                        } else {
                            if (distanceBetween(previousLatLng!!, latlang) > 3f) {
                                updateCarLocation(latlang, location.bearing, location.hasBearing())


                            }
                        }

                    }

                }
            }, null)
        }
    }

    private fun distanceBetween(latLng1: LatLng, latLng2: LatLng): Float {
        val loc1 = Location(LocationManager.GPS_PROVIDER)
        val loc2 = Location(LocationManager.GPS_PROVIDER)
        loc1.latitude = latLng1.latitude
        loc1.longitude = latLng1.longitude
        loc2.latitude = latLng2.latitude
        loc2.longitude = latLng2.longitude
        return loc1.distanceTo(loc2)
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
        var zoom: Float = mMap!!.cameraPosition.zoom
        if (zoom < 15f)
            zoom = 15f
        val cameraPosition = CameraPosition.Builder().target(latLng).zoom(zoom).build()
        mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun animateCamera(latLng: LatLng, bearing: Float) {
        val zoom: Float = mMap!!.cameraPosition.zoom
        val cameraPosition =
            CameraPosition.Builder().target(latLng).bearing(bearing).zoom(zoom).build()
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
                MapUtils.getDestinationBitmap(this, R.drawable.ic_dining_large)
            } else {
                MapUtils.getSourceBitmap(this, R.drawable.ic_user_location)
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
        isDrawFirst = true
    }

    /**
     * This function is used to update the location of the Cab while moving from Origin to Destination
     */
    private fun updateCarLocation(latLng: LatLng, bearing: Float, hasBearing: Boolean) {
        SnapLog.print("updateCarLocation============")
        if (movingCabMarker == null) {
            movingCabMarker = addCarMarkerAndGet(latLng)
        }
        movingCabMarker?.zIndex = 1000F
        if (previousLatLng == null) {
            currentLatLng = latLng
            previousLatLng = currentLatLng
            movingCabMarker?.position = currentLatLng
            movingCabMarker?.setAnchor(0.5f, 0.5f)
            animateCamera(currentLatLng!!)
        } else {
            previousLatLng = currentLatLng
            currentLatLng = latLng


            val updatedLocation = Location(LocationManager.GPS_PROVIDER)
            updatedLocation.latitude = latLng.latitude
            updatedLocation.longitude = latLng.longitude
            moveVechile(movingCabMarker!!, updatedLocation)
            rotateMarker(movingCabMarker!!, bearing, start_rotation, hasBearing)
            /*val valueAnimator = AnimationUtils.carAnimator()
            valueAnimator.addUpdateListener { va ->
                if (currentLatLng != null && previousLatLng != null) {
                    val multiplier = va.animatedFraction


                    val nextLocation = LatLng(
                        multiplier * currentLatLng!!.latitude + (1 - multiplier) * previousLatLng!!.latitude,
                        multiplier * currentLatLng!!.longitude + (1 - multiplier) * previousLatLng!!.longitude
                    )

                    previousLng = currentLng
                    currentLng = nextLocation

                    val updatedLocation = Location(LocationManager.GPS_PROVIDER)
                    updatedLocation.latitude = nextLocation.latitude
                    updatedLocation.longitude = nextLocation.longitude
                    val bearing = bearingBetweenLocations(previousLatLng!!, latLng).toFloat()


                    movingCabMarker?.rotation = bearing
                    SnapLog.print("bearing============$bearing")
                    movingCabMarker?.position = nextLocation
                    movingCabMarker?.setAnchor(0.5f, 0.5f)
                    animateCamera(nextLocation)
                }
            }
            valueAnimator.start()
        }
*/
        }
    }


    /*private fun rotateMarker(marker: Marker, toRotation: Float) {
        if (!isMarkerRotating) {
            val handler = Handler()
            val start: Long = SystemClock.uptimeMillis()
            val startRotation = marker.rotation
            SnapLog.print("startRotation=====$startRotation")

            val duration: Long = 1000
            val interpolator: Interpolator = LinearInterpolator()
            handler.post(object : Runnable {
                override fun run() {
                    isMarkerRotating = true
                    val elapsed: Long = SystemClock.uptimeMillis() - start
                    val t: Float = interpolator.getInterpolation(elapsed.toFloat() / duration)
                    val rot = t * toRotation + (1 - t) * startRotation
                    marker.rotation = if (-rot > 180) rot / 2 else rot
                    SnapLog.print("final=====" + marker.rotation)

                    if (t < 1.0) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16)
                    } else {
                        isMarkerRotating = false
                    }
                }
            })
        }
    }*/
    fun moveVechile(myMarker: Marker, finalPosition: Location) {
        val startPosition = myMarker.position
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val interpolator: Interpolator = AccelerateDecelerateInterpolator()
        val durationInMs = 3000f
        val hideMarker = false
        handler.post(object : Runnable {
            var elapsed: Long = 0
            var t = 0f
            var v = 0f
            override fun run() {
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start
                t = elapsed / durationInMs
                v = interpolator.getInterpolation(t)
                val currentPosition = LatLng(
                    startPosition.latitude * (1 - t) + finalPosition.latitude * t,
                    startPosition.longitude * (1 - t) + finalPosition.longitude * t
                )
                myMarker.setPosition(currentPosition)
                // myMarker.setRotation(finalPosition.getBearing());


                // Repeat till progress is completeelse
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                    // handler.postDelayed(this, 100);
                } else {
                    myMarker.isVisible = !hideMarker
                }
            }
        })
    }

    fun rotateMarker(marker: Marker, toRotation: Float, st: Float, hasBearing: Boolean) {
        if (!hasBearing)
            return
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val startRotation = marker.rotation
        val duration: Long = 1555
        val interpolator: Interpolator = LinearInterpolator()
        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = interpolator.getInterpolation(elapsed.toFloat() / duration)
                val rot = t * toRotation + (1 - t) * startRotation
                marker.rotation = if (-rot > 180) rot / 2 else rot
                start_rotation = if (-rot > 180) rot / 2 else rot
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    fun bearingBetweenLocations(latLng1: LatLng, latLng2: LatLng): Double {
        val PI = 3.14159
        val lat1 = latLng1.latitude * PI / 180
        val long1 = latLng1.longitude * PI / 180
        val lat2 = latLng2.latitude * PI / 180
        val long2 = latLng2.longitude * PI / 180
        val dLon = long2 - long1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - (Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon))
        var brng = Math.atan2(y, x)
        brng = Math.toDegrees(brng)
        brng = (brng + 360) % 360
        return brng
    }


    override fun onDestroy() {
        super.onDestroy()
        stopService(
            Intent(
                this, TrackerService::class.java
            )
        )
    }

}