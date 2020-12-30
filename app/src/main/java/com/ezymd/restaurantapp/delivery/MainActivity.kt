package com.ezymd.restaurantapp.delivery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.work.*
import com.ezymd.restaurantapp.delivery.customviews.SnapTextView
import com.ezymd.restaurantapp.delivery.font.CustomTypeFace
import com.ezymd.restaurantapp.delivery.tracker.TrackerService
import com.ezymd.restaurantapp.delivery.utils.ConnectivityReceiver
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header_new.toolbar
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity(), ConnectivityReceiver.ConnectivityReceiverListener {
    private val PERMISSIONS_REQUEST: Int = 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EzymdApplication.getInstance().loginToFirebase(userInfo!!.userID)
        setGUI()
        setWorkManager()
        setLocationUpdates()
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
        startService(Intent(this, TrackerService::class.java))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

    }


    override fun onResume() {
        super.onResume()
        EzymdApplication.getInstance().setConnectivityListener(this@MainActivity);
    }


    override fun onDestroy() {
        super.onDestroy()


    }

    override fun onStop() {
        super.onStop()
    }

    fun showStudentParentNames() {
        for (i in 0 until 2) {
            val studentName = SnapTextView(this)
            studentName.setTypeface(CustomTypeFace.bold)
            studentName.setSingleLine()
            studentName.textSize = (this as BaseActivity).size!!.loginMediumTextSize
            studentName.setText(
                if (i == 0) {
                    getString(R.string.completed)
                } else {
                    getString(R.string.processing)

                }
            )
            if (i == 0) {
                studentName.setTextColor(ContextCompat.getColor(this, R.color.black))
                tabLayout.addTab(tabLayout.newTab().setCustomView(studentName), true)
            } else {
                studentName.setTextColor(ContextCompat.getColor(this, R.color.gray_999))
                tabLayout.addTab(tabLayout.newTab().setCustomView(studentName))
            }
        }
        tabLayout.post {
            tabLayout.getTabAt(0)?.select()
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    tabSelect(tab.position)
                    mPager.currentItem = tab.position
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }
    }

    private fun tabSelect(position: Int) {
        (tabLayout.getTabAt(position)?.customView as SnapTextView?)?.setTextColor(Color.BLACK)
        for (j in 0 until tabLayout.tabCount) {
            if (j != position) {
                val unSelView = tabLayout.getTabAt(j)?.customView as SnapTextView?
                unSelView?.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.gray_999))
            }
        }
        tabLayout.post { tabLayout.getTabAt(position)?.select() }
    }


    private fun setGUI() {
        setToolbar()
        setAdapter()
        showStudentParentNames()
    }

    private fun setAdapter() {
        mPager.offscreenPageLimit = 2
        val teacherPageAdapter = TeacherPageAdapter(supportFragmentManager)
        mPager.adapter = teacherPageAdapter
        tabLayout!!.setSelectedTabIndicatorColor(
            ContextCompat.getColor(
                this,
                R.color.teacherheader
            )
        )

        mPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                tabSelect(position)
            }
        })

    }

    private fun setToolbar() {
        setSupportActionBar(toolbar)
        headertext.text = userInfo!!.userName
        toolbar.title = userInfo!!.userName
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


    inner class TeacherPageAdapter constructor(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        val mPageReferenceMap = HashMap<Int, Fragment>()
        override fun getItem(position: Int): Fragment {
            val fragment = when (position) {
                0 -> CompletedFragment()
                1 -> ProcessingFragment()
                else -> ProcessingFragment()
            }
            mPageReferenceMap.put(position, fragment)
            return fragment
        }

        override fun getCount(): Int {
            return 2
        }

        override fun destroyItem(container: ViewGroup, position: Int, anyO: Any) {
            super.destroyItem(container, position, anyO)
            mPageReferenceMap.remove(position)
        }
    }


}