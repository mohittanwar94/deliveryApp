package com.ezymd.restaurantapp.delivery

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezymd.restaurantapp.delivery.order.model.OrderModel
import com.ezymd.restaurantapp.delivery.utils.*
import com.ezymd.vendor.order.OrderViewModel
import com.ezymd.vendor.order.adapter.OrdersAdapter
import com.ezymd.vendor.orderdetails.OrderDetailsActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header_new.*

class MainActivity : BaseActivity(), ConnectivityReceiver.ConnectivityReceiverListener {
    private var restaurantAdapter: OrdersAdapter? = null
    private val dataResturant = ArrayList<OrderModel>()
    private val searchViewModel by lazy {
        ViewModelProvider(this).get(OrderViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EzymdApplication.getInstance().loginToFirebase(userInfo!!.userID)
        setGUI()
        setAdapterRestaurant()
        searchViewModel.orderList(BaseRequest(userInfo))
        setObservers()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == JSONKeys.OTP_REQUEST && resultCode == Activity.RESULT_OK) {
            dataResturant.clear()
            restaurantAdapter?.clearData()
            searchViewModel.orderList(BaseRequest(userInfo))
        }
    }

    private fun setAdapterRestaurant() {
        resturantRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        resturantRecyclerView.addItemDecoration(
            VerticalSpacesItemDecoration(
                UIUtil.convertDpToPx(
                    this,
                    resources.getDimension(R.dimen._3sdp)
                )
                    .toInt()
            )
        )
        restaurantAdapter =
            OrdersAdapter(this, object : OnRecyclerViewLongClick {
                override fun onClick(position: Int, view: View?) {
                    startActivityForResult(
                        Intent(this@MainActivity, OrderDetailsActivity::class.java).putExtra(
                            JSONKeys.OBJECT,
                            dataResturant[position]
                        ), JSONKeys.OTP_REQUEST
                    )
                    overridePendingTransition(R.anim.left_in, R.anim.left_out)
                }

                override fun onLongClick(position: Int, view: View?) {
                    val baseRequest = BaseRequest(userInfo!!)
                    baseRequest.paramsMap.put("order_id", "" + dataResturant[position].orderId)
                    searchViewModel.assignOrder(baseRequest)

                }
            }, dataResturant)
        resturantRecyclerView.adapter = restaurantAdapter


    }


    override fun onResume() {
        super.onResume()
        EzymdApplication.getInstance().setConnectivityListener(this@MainActivity);
    }

    private fun setObservers() {


        searchViewModel.isLoading.observe(this, androidx.lifecycle.Observer {
            if (!it) {
                enableEvents()
                progress.visibility = View.GONE
            } else {
                progress.visibility = View.VISIBLE
            }
        })


        searchViewModel.assignResponse.observe(this, Observer {
            if (it != null && it.status == ErrorCodes.SUCCESS) {
                showError(true, it.message, null)
                dataResturant.clear()
                restaurantAdapter?.clearData()
                searchViewModel.orderList(BaseRequest(userInfo))
            } else {
                showError(false, it.message, null)
            }

        })
        searchViewModel.baseResponse.observe(this, androidx.lifecycle.Observer {
            if (it.status == ErrorCodes.SUCCESS && it.data != null) {
                dataResturant.clear()
                restaurantAdapter?.clearData()
                restaurantAdapter?.setData(it.data)
                restaurantAdapter?.getData()?.let { it1 ->
                    dataResturant.addAll(it1)
                }

            } else {
                showError(false, it.message, null)
            }

        })

        searchViewModel.errorRequest.observe(this, androidx.lifecycle.Observer {
            if (it != null)
                showError(false, it, null)
        })


    }

    override fun onDestroy() {
        super.onDestroy()


    }

    override fun onStop() {
        super.onStop()
    }


    private fun setGUI() {
        leftIcon.visibility = View.GONE
        headertext.visibility = View.VISIBLE
        headertext.text = getString(R.string.orders)
        headertext.setTextColor(ContextCompat.getColor(this, R.color.color_002366))
    }


    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        if (!isConnected) {
            noNetworkScreen()
        }
    }

    private fun noNetworkScreen() {
    }


    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.right_in, R.anim.right_out)
    }
}