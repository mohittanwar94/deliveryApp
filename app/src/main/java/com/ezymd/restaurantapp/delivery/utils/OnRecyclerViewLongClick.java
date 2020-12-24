package com.ezymd.restaurantapp.delivery.utils;

import android.view.View;

/**
 * Created by Mohit on 7/25/2016.
 */
public interface OnRecyclerViewLongClick {
    void onClick(int position, View view);

    void onLongClick(int position, View view);
}