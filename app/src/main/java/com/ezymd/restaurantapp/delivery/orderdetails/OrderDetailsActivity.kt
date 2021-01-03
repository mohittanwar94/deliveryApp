package com.ezymd.restaurantapp.delivery.orderdetails

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezymd.restaurantapp.delivery.BaseActivity
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.order.model.OrderItems
import com.ezymd.restaurantapp.delivery.order.model.OrderModel
import com.ezymd.restaurantapp.delivery.order.model.OrderStatus
import com.ezymd.restaurantapp.delivery.orderdetails.adapter.OrderDetailsAdapter
import com.ezymd.restaurantapp.delivery.utils.*
import com.ezymd.vendor.order.OrderViewModel
import kotlinx.android.synthetic.main.activity_order_details.*

class OrderDetailsActivity : BaseActivity() {
    private var actionTaken: Boolean = false
    private var restaurantAdapter: OrderDetailsAdapter? = null
    private val dataResturant = ArrayList<OrderItems>()


    private val item by lazy {
        intent.getSerializableExtra(JSONKeys.OBJECT) as OrderModel
    }


    private val searchViewModel by lazy {
        ViewModelProvider(this).get(OrderViewModel::class.java)
    }

    override fun onBackPressed() {
        if (actionTaken) {
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details)
        setToolBar()
        setHeaderData()
        setAdapter()
        setGUI()
        setObserver()

    }

    private fun setObserver() {
        searchViewModel.isLoading.observe(this, Observer {
            progressBar.visibility = if (it) View.VISIBLE else View.GONE
        })

        searchViewModel.assignResponse.observe(this, Observer {
            if (it != null && it.status == ErrorCodes.SUCCESS) {
                item.orderStatus = OrderStatus.ORDER_ACCEPT_DELIVERY_BOY
                actionTaken = true
                showError(true, it.message, null)
            } else {
                showError(false, it.message, null)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()


    }

    @SuppressLint("SetTextI18n")
    private fun setGUI() {

        if (item.discount != "0.0") {
            discountLay.visibility = View.VISIBLE
            discount.text = getString(R.string.dollor) + String.format("%.2f",item.discount.toDouble())
        }

        order_id.text = getString(R.string.orderID) + " #" + item.orderId
        restaurantname.text = item.restaurantName
        address.text = item.restaurantAddress
        username.text = userInfo?.userName
        order_info.text =
            TimeUtils.getReadableDate(item.created) + " | " + item.orderItems.size + " items | " + getString(
                R.string.dollor
            ) + item.total
        totalAmount.text = getString(R.string.dollor) + item.total
        deliveryInstruction.text = item.deliveryInstruction
        userAddress.text = item.address
        if (item.scheduleType == 2) {
            scheduleAt.text = item.scheduleTime
        } else {
            scheduleAt.text = getString(R.string.now)
        }

        setOrderStatus(item.orderStatus)


        serviceCharge.text = getString(R.string.dollor) + String.format("%.2f",item.transactionCharges.toDouble())

        leftIcon.setOnClickListener {
            onBackPressed()
        }

        if (!item.deliveryCharges.equals("0"))
            shippingCharge.text=getString(R.string.dollor) + String.format("%.2f",item.deliveryCharges.toDouble())
    }

    private fun setOrderStatus(orderStatus: Int) {
        review.visibility = View.GONE
        feedback.visibility = View.GONE
        review.rating = item.delivery_rating.toFloat()
        feedback.text = item.feedback
        if (orderStatus != OrderStatus.ORDER_COMPLETED) {
            status.text = getString(R.string.your_order_processing)
        } else {

            review.visibility = View.VISIBLE
            if (item.feedback != "")
                feedback.visibility = View.VISIBLE
            review.visibility = View.VISIBLE
            status.text = getString(R.string.your_order_is_completed)
        }


    }

    override fun onStart() {
        super.onStart()
        notifyAdapter(item.orderItems)

    }


    private fun setHeaderData() {

        order_id.text = "#" + item.orderId

    }


    override fun onResume() {
        super.onResume()
    }


    private fun setAdapter() {
        resturantRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        resturantRecyclerView.addItemDecoration(
            VerticalSpacesItemDecoration(
                (resources.getDimensionPixelSize(
                    R.dimen._13sdp
                ))
            )
        )
        restaurantAdapter = OrderDetailsAdapter(this, OnRecyclerView { position, view ->

        }, dataResturant)
        resturantRecyclerView.adapter = restaurantAdapter


    }

    override fun onStop() {
        super.onStop()
    }


    private fun notifyAdapter(it: ArrayList<OrderItems>) {
        dataResturant.clear()
        dataResturant.addAll(it)
        restaurantAdapter!!.setData(it)

    }


    private fun setToolBar() {

        setSupportActionBar(findViewById(R.id.toolbar))
    }
}