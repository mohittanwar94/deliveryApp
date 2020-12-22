package com.ezymd.restaurantapp.delivery;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ezymd.restaurantapp.delivery.tracker.TrackerViewModel;
import com.ezymd.restaurantapp.delivery.utils.BaseRequest;
import com.ezymd.restaurantapp.delivery.utils.UserInfo;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class WorkerLocation extends Worker {
    private static final String DEFAULT_START_TIME = "08:00";
    private static final String DEFAULT_END_TIME = "19:00";

    private static final String TAG = "MyWorker";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    /**
     * The current location.
     */
    private Location mLocation;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    private Context mContext;
    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;

    public WorkerLocation(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    private TrackerViewModel viewModel;

    @NonNull
    @Override
    public Result doWork() {
        viewModel = new TrackerViewModel();
        Log.d(TAG, "doWork: Done");
        Log.d(TAG, "onStartJob: STARTING JOB..");

        try {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                }
            };

            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
            mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            try {
                mFusedLocationClient
                        .getLastLocation()
                        .addOnCompleteListener(new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    mLocation = task.getResult();
                                    Log.d(TAG, "Location : " + mLocation);
                                    if (mLocation != null) {
                                        UserInfo userInfo = UserInfo.getInstance(getApplicationContext());
                                        BaseRequest baseRequest = new BaseRequest(userInfo);
                                        baseRequest.paramsMap.put("lat", "" + mLocation.getLatitude());
                                        baseRequest.paramsMap.put("lang", "" + mLocation.getLatitude());
                                        baseRequest.paramsMap.put("user_id", "" + userInfo.getUserID());
                                        viewModel.downloadLatestCoordinates(baseRequest);
                                        // baseRequest.paramsMap.put("order_id",""+location.get());
                                    }


                                    mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                                } else {
                                    Log.w(TAG, "Failed to get location.");
                                }
                            }
                        });
            } catch (SecurityException unlikely) {
                Log.e(TAG, "Lost location permission." + unlikely);
            }

            try {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, null);
            } catch (SecurityException unlikely) {
                //Utils.setRequestingLocationUpdates(this, false);
                Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();

        }

        return Result.success();
    }
}