package com.ezymd.restaurantapp.delivery.orderdetails

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezymd.restaurantapp.delivery.BaseActivity
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.login.LoginRepository
import com.ezymd.restaurantapp.delivery.order.model.OrderItems
import com.ezymd.restaurantapp.delivery.order.model.OrderModel
import com.ezymd.restaurantapp.delivery.order.model.OrderStatus
import com.ezymd.restaurantapp.delivery.order.viewmodel.OrderListRepository
import com.ezymd.restaurantapp.delivery.orderdetails.adapter.OrderDetailsAdapter
import com.ezymd.restaurantapp.delivery.utils.*
import com.ezymd.restaurantapp.delivery.order.viewmodel.OrderViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_order_details.*

@AndroidEntryPoint
class OrderDetailsActivity : BaseActivity() {
    private var actionTaken: Boolean = false
    private var restaurantAdapter: OrderDetailsAdapter? = null
    private val dataResturant = ArrayList<OrderItems>()


    private val item by lazy {
        intent.getSerializableExtra(JSONKeys.OBJECT) as OrderModel
    }


    private val searchViewModel: OrderViewModel by viewModels()


    override fun onBackPressed() {
        if (actionTaken) {
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            super.onBackPressed()
        }
        overridePendingTransition(R.anim.right_in, R.anim.right_out)
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

    private fun getTotalPrice(arrayList: ArrayList<OrderItems>): Double {
        var price = 0.0
        for (itemModel in arrayList) {
            price += (itemModel.price * itemModel.qty)
        }



        return price
    }

    private fun getPaymentMode(paymentType: Int): String {
        if (paymentType == PaymentMethodTYPE.COD)
            return getString(R.string.cash_on_delivery)
        else if (paymentType == PaymentMethodTYPE.ONLINE)
            return getString(R.string.card)
        else
            return getString(R.string.wallet)
    }

    @SuppressLint("SetTextI18n")
    private fun setGUI() {
        paymentMode.text = getPaymentMode(item.paymentType)
        subTotal.text =
            item.currency+ String.format("%.2f", getTotalPrice(item.orderItems))

        if (item.discount != "0.0") {
            discountLay.visibility = View.VISIBLE
            discount.text =
                item.currency + String.format("%.2f", item.discount.toDouble())
        }

        order_id.text = getString(R.string.orderID) + " #" + item.orderId
        restaurantname.text = item.restaurantName
        address.text = item.restaurantAddress
        username.text = item.username
        order_info.text =
            TimeUtils.getReadableDate(item.created) + " | " + item.orderItems.size + " items | " + item.currency + item.total
        totalAmount.text = item.currency + item.total
        deliveryInstruction.text = item.deliveryInstruction
        userAddress.text = item.address
        if (item.scheduleType == 2) {
            scheduleAt.text = item.scheduleTime
        } else {
            scheduleAt.text = getString(R.string.now)
        }

        setOrderStatus(item.orderStatus)


        //serviceCharge.text = getString(R.string.dollor) + String.format("%.2f",item.transactionCharges.toDouble())

        leftIcon.setOnClickListener {
            onBackPressed()
        }

        if (!item.deliveryCharges.equals("0"))
            shippingCharge.text =
                item.currency+ String.format("%.2f", item.deliveryCharges.toDouble())
    }

    private fun setOrderStatus(orderStatus: Int) {
        review.visibility = View.GONE
        feedback.visibility = View.GONE
        review.rating = item.delivery_rating.toFloat()
        feedback.text = item.feedback
        if (orderStatus == OrderStatus.ORDER_CANCEL) {
            status.text = getString(R.string.your_order_cancel)
            status.setTextColor(Color.RED)
        } else if (orderStatus == OrderStatus.ORDER_COMPLETED) {
            review.visibility = View.VISIBLE
            if (item.feedback != "")
                feedback.visibility = View.VISIBLE
            review.visibility = View.VISIBLE
            status.text = getString(R.string.your_order_is_completed)
        } else {
            status.text = getString(R.string.your_order_processing)
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