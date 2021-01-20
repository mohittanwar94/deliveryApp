package com.ezymd.restaurantapp.delivery.order

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ezymd.restaurantapp.delivery.BaseActivity
import com.ezymd.restaurantapp.delivery.EzymdApplication
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.order.model.OrderModel
import com.ezymd.restaurantapp.delivery.order.model.OrderStatus
import com.ezymd.restaurantapp.delivery.tracker.TrackerViewModel
import com.ezymd.restaurantapp.delivery.utils.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.ncorti.slidetoact.SlideToActView
import com.view.circulartimerview.CircularTimerListener
import com.view.circulartimerview.TimeFormatEnum
import kotlinx.android.synthetic.main.tracker_activity.*
import kotlinx.android.synthetic.main.user_live_tracking.*


class IncomingOrderActivity : BaseActivity(), OnMapReadyCallback {


    private var mMap: GoogleMap? = null
    private val trackViewModel by lazy { ViewModelProvider(this).get(TrackerViewModel::class.java) }
    private var actionTaken = false
    private var isBackEnable = false


    private val orderModel by lazy { intent.getSerializableExtra(JSONKeys.OBJECT) as OrderModel }

    override fun onBackPressed() {
        if (isBackEnable) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tracker_activity)
        try {
            val uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.sound)
            val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        setGUI()
        setObserver()

    }

    @SuppressLint("SetTextI18n")
    private fun setGUI() {
        restCall.visibility = View.GONE
        name.text = orderModel.restaurantName
        address.text = orderModel.restaurantAddress

        progress_circular.setPrefix("")
        progress_circular.setCircularTimerListener(object : CircularTimerListener {
            override fun updateDataOnTick(remainingTimeInMs: Long): String? {
                progress_circular.progress =
                    Math.ceil((remainingTimeInMs / 1000f).toDouble()).toFloat()
                return (Math.ceil((remainingTimeInMs / 1000f).toDouble()).toInt()).toString()
            }

            override fun onTimerFinished() {
                progress_circular.setPrefix("")
                progress_circular.setSuffix("")
                isBackEnable = true
                if (!actionTaken)
                    finish()
            }
        }, 60, TimeFormatEnum.SECONDS, 1)

        progress_circular.setMaxValue(60f)
        progress_circular.startTimer()


        val slideListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                actionTaken = true
                progress_circular.stopTimer()
                acceptOrder()
            }

        }

        accept.typeFace = Typeface.BOLD
        accept.onSlideCompleteListener = slideListener

        if (orderModel.paymentType == PaymentMethodTYPE.COD)
            cash.visibility = View.VISIBLE

    }

    private fun setObserver() {

        trackViewModel.showError().observe(this, Observer {
            if (it != null) {
                showError(false, it, null)
                acceptOrder()
            }
        })

        trackViewModel.isLoading().observe(this, Observer {
            if (it != null) {
                progress.visibility = if (it) View.VISIBLE else View.GONE
            }
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
                    this.finish()
                }

            }
        })


    }

    private fun acceptOrder() {
        progress_circular.stopTimer()
        val baseRequest = BaseRequest(userInfo)
        baseRequest.paramsMap["order_id"] = "" + orderModel.orderId
        baseRequest.paramsMap["order_status"] = "" + OrderStatus.ORDER_ACCEPT_DELIVERY_BOY
        baseRequest.paramsMap["firebase_path"] =
            FireBaseConstants.path + userInfo!!.userID + "/" + orderModel.key
        trackViewModel.acceptOrder(baseRequest)
    }


    override fun onMapReady(map: GoogleMap) {
        mMap = map
        mMap!!.setMaxZoomPreference(16f)
        val defaultLocation =
            LatLng(orderModel.restaurant_lat.toDouble(), orderModel.restaurant_lang.toDouble())
        mMap!!.uiSettings.isMyLocationButtonEnabled = false
        showDefaultLocationOnMap(defaultLocation)

    }

    private fun showDefaultLocationOnMap(defaultLocation: LatLng) {
        moveCamera(defaultLocation)
        animateCamera(defaultLocation)
    }

    private fun moveCamera(latLng: LatLng) {
        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun animateCamera(latLng: LatLng) {
        val cameraPosition = CameraPosition.Builder().target(latLng).zoom(16f).build()
        mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

}