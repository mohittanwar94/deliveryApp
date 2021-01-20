package com.ezymd.restaurantapp.delivery

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ezymd.restaurantapp.delivery.order.model.OrderModel
import com.ezymd.restaurantapp.delivery.orderdetails.OrderDetailsActivity
import com.ezymd.restaurantapp.delivery.utils.*
import com.ezymd.vendor.order.OrderViewModel
import com.ezymd.vendor.order.adapter.OrdersAdapter
import kotlinx.android.synthetic.main.fragment_orders.*

class CancelledFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private var restaurantAdapter: OrdersAdapter? = null
    private var isNullViewRoot = false
    private lateinit var searchViewModel: OrderViewModel
    private var viewRoot: View? = null

    private val dataResturant = ArrayList<OrderModel>()

    private val userInfo by lazy {
        (activity as MainActivity).userInfo
    }

    override fun onRefresh() {
        swipeLayout.isRefreshing = true
        dataResturant.clear()
        restaurantAdapter?.clearData()
        val baseRequest = BaseRequest(userInfo)
        baseRequest.paramsMap["order_status"] = "cancelled"
        baseRequest.paramsMap["delivery_boy_id"] = ""+userInfo?.userID
        searchViewModel.orderList(baseRequest)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        searchViewModel = ViewModelProvider(this).get(OrderViewModel::class.java)
        isNullViewRoot = false
        if (viewRoot == null) {
            viewRoot = inflater.inflate(R.layout.fragment_orders, container, false)
            isNullViewRoot = true
        }
        return viewRoot
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        SnapLog.print("onActivityCreated")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isNullViewRoot) {
            setAdapterRestaurant()
            val baseRequest = BaseRequest(userInfo)
            baseRequest.paramsMap["order_status"] = "cancelled"
            baseRequest.paramsMap["delivery_boy_id"] = ""+userInfo?.userID
            searchViewModel.orderList(baseRequest)
            setObservers()

        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == JSONKeys.OTP_REQUEST && resultCode == Activity.RESULT_OK) {
            dataResturant.clear()
            restaurantAdapter?.clearData()
            val baseRequest = BaseRequest(userInfo)
            baseRequest.paramsMap["order_status"] = "cancelled"
            baseRequest.paramsMap["delivery_boy_id"] = ""+userInfo?.userID
            searchViewModel.orderList(baseRequest)
        }
    }

    private fun setAdapterRestaurant() {
        swipeLayout.setOnRefreshListener(this)
        resturantRecyclerView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        resturantRecyclerView.addItemDecoration(
            VerticalSpacesItemDecoration(
                UIUtil.convertDpToPx(
                    activity,
                    requireActivity().resources.getDimension(R.dimen._3sdp)
                )
                    .toInt()
            )
        )
        restaurantAdapter =
            OrdersAdapter(activity as MainActivity, object : OnRecyclerViewLongClick {
                override fun onClick(position: Int, view: View?) {
                    activity!!.startActivityFromFragment(
                        this@CancelledFragment,
                        Intent(requireActivity(), OrderDetailsActivity::class.java).putExtra(
                            JSONKeys.OBJECT,
                            dataResturant[position]
                        ), JSONKeys.OTP_REQUEST
                    )
                    requireActivity().overridePendingTransition(R.anim.left_in, R.anim.left_out)
                }

                override fun onLongClick(position: Int, view: View?) {


                }
            }, dataResturant)
        resturantRecyclerView.adapter = restaurantAdapter


    }

    override fun onResume() {
        super.onResume()
    }

    private fun setObservers() {
        searchViewModel.isLoading.observe(requireActivity(), androidx.lifecycle.Observer {
            if (!it) {
                (activity as BaseActivity).enableEvents()
                progress.visibility = View.GONE
                swipeLayout.isRefreshing = false
            } else {
                progress.visibility = View.VISIBLE
            }
        })

        EzymdApplication.getInstance().isRefresh.observe(requireActivity(), Observer {
            if (it) {
                dataResturant.clear()
                restaurantAdapter?.clearData()
                val baseRequest = BaseRequest(userInfo)
                baseRequest.paramsMap["order_status"] = "complete_for_restaurant"
                searchViewModel.orderList(baseRequest)
            }
        })
        searchViewModel.baseResponse.observe(requireActivity(), androidx.lifecycle.Observer {
            if (it.status == ErrorCodes.SUCCESS && it.data != null) {
                dataResturant.clear()
                restaurantAdapter?.clearData()
                restaurantAdapter?.setData(it.data)
                restaurantAdapter?.getData()?.let { it1 ->
                    dataResturant.addAll(it1)
                }

                showEmpty(dataResturant.size)

            } else {
                (activity as BaseActivity).showError(false, it.message, null)
            }

        })

        searchViewModel.errorRequest.observe(this, androidx.lifecycle.Observer {
            (activity as BaseActivity).showError(false, it, null)
        })


    }

    override fun onDestroy() {
        super.onDestroy()

        searchViewModel.isLoading.removeObservers(this)
        searchViewModel.errorRequest.removeObservers(this)
        searchViewModel.baseResponse.removeObservers(this)
    }


    override fun onStop() {
        super.onStop()
    }

    fun showEmpty(size: Int) {
        emptyView.visibility = if (size == 0) View.VISIBLE else View.GONE
    }


}