/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ezymd.restaurantapp.delivery.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.ezymd.restaurantapp.delivery.EzymdApplication;
import com.ezymd.restaurantapp.delivery.R;
import com.ezymd.restaurantapp.delivery.order.IncomingOrderActivity;
import com.ezymd.restaurantapp.delivery.order.model.OrderModel;
import com.ezymd.restaurantapp.delivery.utils.JSONKeys;
import com.ezymd.restaurantapp.delivery.utils.SnapLog;
import com.ezymd.restaurantapp.delivery.utils.UserInfo;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import org.json.JSONException;

import java.util.Map;


public class MyFirebaseMessagingService extends FirebaseMessagingService {
    public static int notification_count = 0;

    @Override
    public void onNewToken(String refreshedToken) {
        super.onNewToken(refreshedToken);
        UserInfo sd = UserInfo.getInstance(this);
        if (!TextUtils.isEmpty(refreshedToken)) {
            sd.setDeviceToken(refreshedToken);

        }
        SnapLog.print(sd.getDeviceToken());
    }


    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        try {
            UserInfo userInfo = UserInfo.getInstance(this);
            if (userInfo.getUserID() == 0)
                return;
            Map<String, String> data = remoteMessage.getData();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                SnapLog.print(entry.getKey() + ":" + entry.getValue());
            }
            generateNotification(this, remoteMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateNotification(Context context, RemoteMessage remoteData) throws JSONException {
        Map<String, String> data = remoteData.getData();
        String value = data.get("order");
        if (EzymdApplication.isAppForeground(this)) {
            OrderModel orderModel = new Gson().fromJson(value, OrderModel.class);
            SnapLog.print(value);
            Intent mIntent = new Intent(this, IncomingOrderActivity.class);
            mIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            mIntent.putExtra(JSONKeys.OBJECT, orderModel);
            startActivity(mIntent);
        } else {
            OrderModel orderModel = new Gson().fromJson(value, OrderModel.class);
            String subTitle = data.get(JSONKeys.SUB_TITLE);
            String title = data.get(JSONKeys.TITLE);
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), getString(R.string.default_notification_channel_id));
            notification.setPriority(NotificationCompat.PRIORITY_MAX);
            Intent activityIntent;

            notification.setContentTitle(title);
            notification.setSmallIcon(R.drawable.ic_location);
            notification.setAutoCancel(true);
            notification.setTimeoutAfter(60000L);
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(title);
            notification.setContentText(subTitle);
            bigTextStyle.bigText(subTitle);
            notification.setStyle(bigTextStyle);

            try {
                Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sound);
                Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
                ringtone.play();
            } catch (Exception e) {
                e.printStackTrace();
            }

            activityIntent = new Intent(context, IncomingOrderActivity.class);
            activityIntent.putExtra(JSONKeys.OBJECT, orderModel);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(IncomingOrderActivity.class);
            stackBuilder.addNextIntent(activityIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            notification.setContentIntent(resultPendingIntent);


            notificationManager.notify((int) System.currentTimeMillis(), notification.build());
        }

    }


}