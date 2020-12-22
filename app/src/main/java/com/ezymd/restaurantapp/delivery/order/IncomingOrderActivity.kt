package com.ezymd.restaurantapp.delivery.order

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ezymd.restaurantapp.delivery.BaseActivity
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.tracker.TrackerViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
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

    override fun onBackPressed() {
        if (isBackEnable) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tracker_activity)
        setGUI()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)


    }

    @SuppressLint("SetTextI18n")
    private fun setGUI() {
        name.text = ""
        address.text =
            ""


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
            }

        }

        accept.onSlideCompleteListener = slideListener

    }

    private fun setObserver() {

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
        mMap!!.setMaxZoomPreference(16f)
        mMap!!.uiSettings.isMyLocationButtonEnabled = true
        setObserver()
    }


}