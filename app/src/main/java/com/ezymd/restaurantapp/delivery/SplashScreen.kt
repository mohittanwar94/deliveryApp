package com.ezymd.restaurantapp.delivery

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.ezymd.restaurantapp.delivery.login.Login
import com.ezymd.restaurantapp.delivery.order.IncomingOrderActivity
import com.ezymd.restaurantapp.delivery.order.ReachPickUpOrderActivity
import com.ezymd.restaurantapp.delivery.tracker.TrackerActivity
import com.ezymd.restaurantapp.delivery.utils.SnapLog
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class SplashScreen : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {

        val mNotificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /*  val audioAttributes = AudioAttributes.Builder()
                  .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                  .setUsage(AudioAttributes.USAGE_ALARM)
                  .build()
            */
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(

                getString(R.string.NOTIFICATION_CHANNEL_ID),
                getString(R.string.notification_channel_name),
                importance
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern =
                longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            //notificationChannel.setSound(sound, audioAttributes)
            mNotificationManager.createNotificationChannel(notificationChannel)
        }


    }

    override fun onResume() {
        super.onResume()
        printKeyHash(this)
        if (userInfo!!.userID != 0)
            startActivity(Intent(this, ReachPickUpOrderActivity::class.java))
        else
            startActivity(Intent(this, Login::class.java))

        overridePendingTransition(R.anim.left_in, R.anim.left_out)
        this.finish()
    }

    fun printKeyHash(context: Activity): String? {
        val packageInfo: PackageInfo
        var key: String? = null
        try {
            //getting application package name, as defined in manifest
            val packageName = context.applicationContext.packageName

            //Retriving package info
            packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            Log.e("Package Name=", context.applicationContext.packageName)
            for (signature in packageInfo.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                key = String(Base64.encode(md.digest(), 0))

                // String key = new String(Base64.encodeBytes(md.digest()));
                SnapLog.print("Key Hash=" + key)
            }
        } catch (e1: PackageManager.NameNotFoundException) {
            Log.e("Name not found", e1.toString())
        } catch (e: NoSuchAlgorithmException) {
            Log.e("No such an algorithm", e.toString())
        } catch (e: Exception) {
            Log.e("Exception", e.toString())
        }
        return key
    }
}