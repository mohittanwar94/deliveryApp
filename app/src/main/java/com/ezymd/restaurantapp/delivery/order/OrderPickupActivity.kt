package com.ezymd.restaurantapp.delivery.order

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ezymd.restaurantapp.delivery.BaseActivity
import com.ezymd.restaurantapp.delivery.EzymdApplication
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.customviews.SnapTextView
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.ncorti.slidetoact.SlideToActView
import kotlinx.android.synthetic.main.header_new.*
import kotlinx.android.synthetic.main.order_details.*
import kotlinx.android.synthetic.main.order_item_pick_up.*


class OrderPickupActivity : BaseActivity(), OnMapReadyCallback {
    private var markAsSpamDialogFragment: ItemsDialogFragment? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null

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
        setContentView(R.layout.order_item_pick_up)
        setGUI()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)


    }

    @SuppressLint("SetTextI18n")
    private fun setGUI() {
        name.text = orderModel.restaurantName
        address.text = orderModel.restaurantAddress

        setItems(items)
        accept.text = getString(R.string.pick_the_order)
        headertext.text = getString(R.string.pick_order)
        headertext.visibility = View.VISIBLE
        val slideListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                showItemDialog()
                accept.resetSlider()

            }

        }
        accept.typeFace = Typeface.BOLD
        accept.onSlideCompleteListener = slideListener

        leftIcon.setOnClickListener {
            UIUtil.clickHandled(it)
            onBackPressed()
        }

        setOrderDetails()

    }

    private fun showItemDialog() {
        markAsSpamDialogFragment?.dismissAllowingStateLoss()
        markAsSpamDialogFragment = ItemsDialogFragment.newInstance(orderModel.orderItems)
        markAsSpamDialogFragment!!.show(supportFragmentManager, "fragment_mark_as_spam")
        markAsSpamDialogFragment!!.setOnClickListener {
            acceptOrder()
        }


    }


    private fun acceptOrder() {
        val baseRequest = BaseRequest(userInfo)
        baseRequest.paramsMap["order_id"] = "" + orderModel.orderId
        baseRequest.paramsMap["order_status"] = "" + OrderStatus.ITEMS_PICKED_FROM_RESTAURANT
        trackViewModel.acceptOrder(baseRequest)
    }


    private fun setItems(itemInfo: SnapTextView) {
        val builder = StringBuilder("")
        for (item in orderModel.orderItems) {
            builder.append(item.item)
            builder.append(" x ")
            builder.append(item.qty)
            builder.append("\n")
        }

        itemInfo.text = builder.toString()
    }

    private fun setOrderDetails() {
        val spannable = SpannableString(getString(R.string.show_order_details))
        spannable.setSpan(UnderlineSpan(), 0, spannable.length, 0)
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.color_ffb912)),
            0,
            spannable.length,
            0
        )
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
                spannable.setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            this,
                            R.color.color_ffb912
                        )
                    ), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
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
                spannable.setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            this,
                            R.color.color_ffb912
                        )
                    ), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                showDetails.text = spannable
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

        trackViewModel.acceptRequest.observe(this, Observer {
            if (it != null) {
                if (it.status != ErrorCodes.SUCCESS) {
                    showError(false, it.message, null)
                } else {
                    EzymdApplication.getInstance().isRefresh.postValue(true)
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    startActivityForResult(
                        Intent(
                            this@OrderPickupActivity,
                            CompleteOrderActivity::class.java
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

        trackViewModel.isLoading.observe(this, Observer {
            progress.visibility = if (it) {
                View.VISIBLE
            } else {
                View.GONE
            }
        })

    }


    override fun onMapReady(map: GoogleMap) {
        mMap = map
        mMap!!.setMaxZoomPreference(16f)
        mMap!!.uiSettings.isMyLocationButtonEnabled = false
        setObserver()
        val latLng =
            LatLng(orderModel.restaurant_lat.toDouble(), orderModel.restaurant_lang.toDouble())
        showMarkersOnMap(latLng)
        showDefaultLocationOnMap(latLng)
    }

    private fun addOriginDestinationMarkerAndGet(isSource: Boolean, latLng: LatLng): Marker {
        val bitmapDescriptor =
            if (isSource) {
                MapUtils.getSourceBitmap(this, R.drawable.ic_delivery_man)
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

    private fun moveCamera(latLng: LatLng) {
        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun animateCamera(latLng: LatLng) {
        val cameraPosition = CameraPosition.Builder().target(latLng).zoom(15.5f).build()
        mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun showMarkersOnMap(latLng: LatLng) {
        destinationMarker = addOriginDestinationMarkerAndGet(false, latLng)
        destinationMarker?.setAnchor(0.5f, 0.5f)
        destinationMarker?.isDraggable = false

        originMarker = addOriginDestinationMarkerAndGet(true, latLng)
        originMarker?.setAnchor(0.5f, 0.5f)
        originMarker?.isDraggable = false


    }


}