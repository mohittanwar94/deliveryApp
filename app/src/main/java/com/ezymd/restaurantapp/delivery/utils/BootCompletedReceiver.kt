package com.ezymd.restaurantapp.delivery.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.ezymd.restaurantapp.delivery.WorkerLocation
import java.util.concurrent.TimeUnit


class BootCompletedReceiver : BroadcastReceiver() {


    private fun constraints(): Constraints {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false).build()
        return constraints;
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            SnapLog.print("onReceive call onReceive ACTION_BOOT_COMPLETED")
            val mWorkManager = WorkManager.getInstance(context)
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
    }


}