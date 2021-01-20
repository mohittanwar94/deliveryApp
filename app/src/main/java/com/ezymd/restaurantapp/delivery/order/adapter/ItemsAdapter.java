package com.ezymd.restaurantapp.delivery.order.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;

import com.ezymd.restaurantapp.delivery.R;
import com.ezymd.restaurantapp.delivery.customviews.SnapTextView;
import com.ezymd.restaurantapp.delivery.order.model.OrderItems;

import java.util.ArrayList;
import java.util.Objects;

public class ItemsAdapter extends BaseAdapter {
    private ArrayList<OrderItems> mData;
    private Context mContext;

    public ItemsAdapter(ArrayList<OrderItems> aldata, Context context) {
        this.mData = aldata;
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint({"InflateParams", "SetTextI18n"})
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = Objects.requireNonNull(mInflater).inflate(R.layout.order_items_check, null);
        }
        OrderItems item = mData.get(position);
        SnapTextView name = convertView.findViewById(R.id.name);
        name.setText(item.getItem());
        SnapTextView qty = convertView.findViewById(R.id.qty);
        qty.setText("Qty - " + item.getQty());
        CheckBox chkbox = convertView.findViewById(R.id.chkbox);
        chkbox.setClickable(false);
        convertView.setOnClickListener(v -> {
            OrderItems itme = mData.get(position);

            if (chkbox.isChecked()) {
                chkbox.setChecked(false);
                itme.setSelected(false);
            } else {
                chkbox.setChecked(true);
                itme.setSelected(true);
            }
            mData.set(position, itme);
        });
        return convertView;
    }

    public ArrayList<OrderItems> getData() {

        return mData;
    }

}
