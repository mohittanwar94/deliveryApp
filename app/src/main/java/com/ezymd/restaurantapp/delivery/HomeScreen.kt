package com.ezymd.restaurantapp.delivery

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.plusAssign
import androidx.navigation.ui.setupWithNavController
import androidx.work.*
import com.ezymd.restaurantapp.delivery.tracker.TrackerActivity.PERMISSIONS_REQUEST
import com.ezymd.restaurantapp.delivery.utils.ConnectivityReceiver
import com.ezymd.restaurantapp.delivery.utils.KeepStateNavigator
import com.ezymd.restaurantapp.delivery.utils.SnapLog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode.LABEL_VISIBILITY_LABELED
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class HomeScreen : BaseActivity(), ConnectivityReceiver.ConnectivityReceiverListener {

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)


        val navController = findNavController(R.id.nav_host_fragment)

        // get fragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)!!

        // setup custom navigator
        val navigator =
            KeepStateNavigator(this, navHostFragment.childFragmentManager, R.id.nav_host_fragment)
        navController.navigatorProvider += navigator

        // set navigation graph
        navController.setGraph(R.navigation.mobile_navigation)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        navView.setupWithNavController(navController)
        navView.labelVisibilityMode = LABEL_VISIBILITY_LABELED
        SnapLog.print("onCreate")

        setWorkManager()
        setLocationUpdates()
        if (userInfo!!.userID != 0)
            EzymdApplication.getInstance().loginToFirebase(userInfo!!.userID)
    }

    private fun setLocationUpdates() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show()
        }

        // Check location permission is granted - if it is, start
        // the service, otherwise request the permission
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        );
        if (permission == PackageManager.PERMISSION_GRANTED) {
            startTrackerService()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                PERMISSIONS_REQUEST
            )
        }
    }


    private fun setWorkManager() {
        val mWorkManager = WorkManager.getInstance(this)
        val someWork = PeriodicWorkRequest.Builder(
            WorkerLocation::class.java, 15, TimeUnit.MINUTES
        )
            .setConstraints(constraints())
            .addTag("LOCATION")
            .build()
        mWorkManager.enqueueUniquePeriodicWork(
            "LOCATION",
            ExistingPeriodicWorkPolicy.KEEP,
            someWork
        )
    }

    private fun constraints(): Constraints {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false).build()
        return constraints;
    }

    private fun startTrackerService() {
        //  startService(Intent(this, TrackerService::class.java))
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST && grantResults.size == 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Start the service when the permission is granted
            startTrackerService();
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        SnapLog.print("onNewIntent")
    }

    override fun onResume() {
        super.onResume()
        EzymdApplication.getInstance().setConnectivityListener(this@HomeScreen);
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
    }
}