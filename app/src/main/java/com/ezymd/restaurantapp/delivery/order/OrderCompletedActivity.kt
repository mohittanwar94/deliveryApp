package com.ezymd.restaurantapp.delivery.order

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ezymd.restaurantapp.delivery.BaseActivity
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.tracker.TrackerViewModel
import com.ezymd.restaurantapp.delivery.utils.UIUtil
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.android.synthetic.main.header_new.*
import kotlinx.android.synthetic.main.order_completed_details_with_customer.*


class OrderCompletedActivity : BaseActivity(), OnMapReadyCallback {

    private var showingDetails = false
    private var mMap: GoogleMap? = null
    private val trackViewModel by lazy { ViewModelProvider(this).get(TrackerViewModel::class.java) }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.order_completed)
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

        items.text = ""
        headertext.text = getString(R.string.order_completed)
        headertext.visibility = View.VISIBLE

        leftIcon.setOnClickListener {
            UIUtil.clickHandled(it)
            onBackPressed()
        }

        setOrderDetails()

    }

    private fun setOrderDetails() {
        val spannable = SpannableString(getString(R.string.show_order_details))
        spannable.setSpan(UnderlineSpan(), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        showDetails.text = spannable.toString()
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
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                showDetails.text = spannable.toString()
            } else {
                items.visibility = View.GONE
                val spannable = SpannableString(getString(R.string.show_order_details))
                spannable.setSpan(
                    UnderlineSpan(),
                    0,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                showDetails.text = spannable.toString()
            }

        }
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
        mMap!!.uiSettings.isMyLocationButtonEnabled = false
        setObserver()
    }


}