package com.ezymd.restaurantapp.delivery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ezymd.restaurantapp.delivery.location.BackgroundLocationService;
import com.ezymd.restaurantapp.delivery.utils.SnapLog;

public class LocationPublisher extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SnapLog.print("onReceive LocationPublisher ");
        Intent intent1 = new Intent(context, BackgroundLocationService.class);
        context.startService(intent1);

    }
}
