package com.ezymd.restaurantapp.delivery;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.MutableLiveData;

import com.ezymd.restaurantapp.delivery.order.IncomingOrderActivity;
import com.ezymd.restaurantapp.delivery.order.model.OrderModel;
import com.ezymd.restaurantapp.delivery.order.model.OrderStatus;
import com.ezymd.restaurantapp.delivery.utils.AppSignatureHelper;
import com.ezymd.restaurantapp.delivery.utils.ConnectivityReceiver;
import com.ezymd.restaurantapp.delivery.utils.FireBaseConstants;
import com.ezymd.restaurantapp.delivery.utils.JSONKeys;
import com.ezymd.restaurantapp.delivery.utils.SnapLog;
import com.ezymd.restaurantapp.delivery.utils.TimeUtils;
import com.ezymd.restaurantapp.delivery.utils.UserInfo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


/**
 * Created by Mohit on 10/17/20202.
 */
public class EzymdApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private Activity mLastForegroundActivity;
    public MutableLiveData<Boolean> isRefresh = new MutableLiveData<>();
    @Nullable
    public final String networkErrorMessage = "it seems network is not available right now";

    public static boolean isAppForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        if (procInfos != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && processInfo.processName.equals(context.getApplicationContext().getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean isAppRunning(final Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        if (procInfos != null) {
            for (final ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
                if (processInfo.processName.equals(context.getApplicationContext().getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    private static EzymdApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        mInstance = this;
        UserInfo userInfo = UserInfo.getInstance(mInstance);
        AppSignatureHelper appSignatureHelper = new AppSignatureHelper(this);
        appSignatureHelper.getAppSignatures();
        if (getResources() == null) {
            Process.killProcess(Process.myPid());
        }

        if (userInfo.getDeviceID().length() < 1) {
            String deviceId = android.provider.Settings.System.getString(getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);
            userInfo.setDeviceID(deviceId == null ? "" + new ApplicationInfo().uid : deviceId);
        }
    }


    public void loginToFirebase(int userID) {
        // Authenticate with Firebase and subscribe to updates
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
                FireBaseConstants.email, FireBaseConstants.password
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    SnapLog.print(FireBaseConstants.path + userID);
                    subscribeToUpdates(FireBaseConstants.path + userID);
                    SnapLog.print(FireBaseConstants.path + userID);
                    SnapLog.print("firebase auth success");
                } else {
                    SnapLog.print("firebase auth failed");
                }

            }
        });
    }

    private void subscribeToUpdates(String path) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {
                processNewOrder(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void processNewOrder(DataSnapshot dataSnapshot) {
        if (dataSnapshot == null)
            return;
        JSONObject json = new JSONObject((Map) dataSnapshot.getValue());
        OrderModel user =
                new Gson().fromJson(
                        json.toString(), OrderModel.class);

        user.setKey(dataSnapshot.getKey());
        if (TimeUtils.isOrderLive(dataSnapshot.getKey()) && user.getOrderStatus() == OrderStatus.ORDER_ACCEPTED) {
            Intent intent = new Intent(getApplicationContext(), IncomingOrderActivity.class);
            intent.putExtra(JSONKeys.OBJECT, user);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

    }

    public static synchronized EzymdApplication getInstance() {
        return mInstance;
    }

    public void setConnectivityListener(ConnectivityReceiver.ConnectivityReceiverListener listener) {
        ConnectivityReceiver.connectivityReceiverListener = listener;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
        mLastForegroundActivity = activity;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mLastForegroundActivity = activity;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public Activity getLastForegroundActivity() {
        return mLastForegroundActivity;
    }


}
