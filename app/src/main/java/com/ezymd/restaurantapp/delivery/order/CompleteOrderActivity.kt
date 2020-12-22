package com.ezymd.restaurantapp.delivery.order

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
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
import com.ncorti.slidetoact.SlideToActView
import kotlinx.android.synthetic.main.complete_order.*
import kotlinx.android.synthetic.main.header_new.*
import kotlinx.android.synthetic.main.order_completed_details_with_customer.address
import kotlinx.android.synthetic.main.order_completed_details_with_customer.items
import kotlinx.android.synthetic.main.order_completed_details_with_customer.name
import kotlinx.android.synthetic.main.order_completed_details_with_customer.showDetails
import kotlinx.android.synthetic.main.order_details_with_customer.*


class CompleteOrderActivity : BaseActivity(), OnMapReadyCallback {

    private var showingDetails = false
    private var mMap: GoogleMap? = null
    private val trackViewModel by lazy { ViewModelProvider(this).get(TrackerViewModel::class.java) }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.complete_order)
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
        accept.text = getString(R.string.complete_the_order)
        headertext.text = getString(R.string.pick_order)
        headertext.visibility = View.VISIBLE
        val slideListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {

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
                    Uri.parse("google.navigation:q=28.7041,77.1025")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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