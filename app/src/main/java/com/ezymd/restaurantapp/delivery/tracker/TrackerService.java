package com.ezymd.restaurantapp.delivery.tracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;

import com.ezymd.restaurantapp.delivery.R;
import com.ezymd.restaurantapp.delivery.utils.BaseRequest;
import com.ezymd.restaurantapp.delivery.utils.JSONKeys;
import com.ezymd.restaurantapp.delivery.utils.UserInfo;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class TrackerService extends LifecycleService {
    private PowerManager.WakeLock wakeLock = null;
    private static final String TAG = TrackerService.class.getSimpleName();
    private int order_id = -1;
    private TrackerViewModel viewModel;

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        super.onStart(intent, startId);
        order_id = intent.getIntExtra(JSONKeys.ID, -1);
    }

    @SuppressLint("ServiceCast")
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        PowerManager manager=(PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"TrackService::lock");
        wakeLock.acquire(30*60*1000L /*30 minutes*/);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        buildNotification();
        viewModel = new TrackerViewModel();
        requestLocationUpdates();
        // loginToFirebase();
    }

    private void buildNotification() {
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        // Create the persistent notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.NOTIFICATION_CHANNEL_ID))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.tracking_location))
                .setOngoing(true)
                .setSilent(true)
                .setContentIntent(broadcastIntent)
                .setSmallIcon(R.drawable.ic_location);
        startForeground(1, builder.build());
    }

    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "received stop broadcast");
            stopSelf();
        }
    };


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }

    private void loginToFirebase() {
        String email = getString(R.string.firebase_email);
        String password = getString(R.string.firebase_password);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
                email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "firebase auth success");
                    requestLocationUpdates();
                } else {
                    Log.d(TAG, "firebase auth failed");
                }
            }
        });
    }

    private void requestLocationUpdates() {
        LocationRequest request = new LocationRequest();
        request.setInterval(5000);
        request.setFastestInterval(3000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        // final String path = getString(R.string.firebase_path) + "/" + getString(R.string.transport_id);
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        updateLocationonServer(location);

                    }
                   /* DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        Log.d(TAG, "location update " + location);
                        ref.setValue(location);
                    }*/


                }
            }, null);
        }

    }

    private void updateLocationonServer(Location location) {
        UserInfo userInfo = UserInfo.getInstance(this);
        BaseRequest baseRequest = new BaseRequest(userInfo);
        baseRequest.paramsMap.put("lat", "" + location.getLatitude());
        baseRequest.paramsMap.put("lang", "" + location.getLongitude());
        baseRequest.paramsMap.put("user_id", "" + userInfo.getUserID());
        baseRequest.paramsMap.put("id", "" + order_id);
        viewModel.downloadLatestCoordinates(baseRequest);
        // baseRequest.paramsMap.put("order_id",""+location.get());
    }


}
