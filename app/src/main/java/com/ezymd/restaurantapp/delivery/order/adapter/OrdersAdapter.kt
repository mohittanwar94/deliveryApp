package com.ezymd.vendor.order.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ezymd.restaurantapp.delivery.R
import com.ezymd.restaurantapp.delivery.order.model.OrderModel
import com.ezymd.restaurantapp.delivery.order.model.OrderStatus
import com.ezymd.restaurantapp.delivery.utils.OnRecyclerViewLongClick
import com.ezymd.restaurantapp.delivery.utils.TimeUtils
import kotlinx.android.synthetic.main.order_item_row.view.*

class OrdersAdapter(
    context: Context,
    onRecyclerViewClick: OnRecyclerViewLongClick,
    dataResturant: ArrayList<OrderModel>
) :
    RecyclerView.Adapter<OrdersAdapter.NotesHolder>() {


    private val onRecyclerView: OnRecyclerViewLongClick = onRecyclerViewClick
    private val data = ArrayList<OrderModel>()


    init {
        data.addAll(dataResturant)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesHolder {
        return NotesHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.order_item_row, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(neData: java.util.ArrayList<OrderModel>) {
        val diffCallback = OrderDiffUtilsCallBack(data, neData)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        data.clear()
        data.addAll(neData)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getData(): ArrayList<OrderModel> {
        return data
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: NotesHolder, position: Int) {


        val item = data[position]
        holder.itemView.order_id.text =
            holder.itemView.order_id.context.getString(R.string.orderID) + " #" + item.orderId
        holder.itemView.name.text = item.restaurantName

        holder.itemView.totalAmount.text =
            item.currency + item.total

        val itemsString = StringBuilder()
        for (model in item.orderItems) {
            itemsString.append(model.item)
            itemsString.append(" x ")
            itemsString.append(model.qty)
            itemsString.append("\n")
        }
        holder.itemView.trackOrder.text = holder.itemView.context.getString(R.string.track_order)
        holder.itemView.items.text = itemsString.toString()
        holder.itemView.created.text = TimeUtils.getReadableDate(item.created)
        if (item.orderStatus == OrderStatus.ORDER_CANCEL) {
            holder.itemView.trackOrder.visibility = View.GONE
        } else if (item.orderStatus == OrderStatus.ORDER_COMPLETED) {
            holder.itemView.trackOrder.visibility = View.GONE
        } else {
            holder.itemView.trackOrder.visibility = View.VISIBLE
            holder.itemView.trackOrder.setOnClickListener {
                onRecyclerView.onLongClick(position, it)
            }
        }


        holder.itemView.setOnClickListener {
            onRecyclerView.onClick(position, it)
        }
    }


    fun clearData() {
        data.clear()
        notifyDataSetChanged()
    }


    inner class NotesHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}