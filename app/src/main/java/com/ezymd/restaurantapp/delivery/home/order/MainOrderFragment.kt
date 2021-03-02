package com.ezymd.restaurantapp.delivery.home.order

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.ezymd.restaurantapp.delivery.*
import com.ezymd.restaurantapp.delivery.customviews.SnapTextView
import com.ezymd.restaurantapp.delivery.font.CustomTypeFace
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_order.*
import java.util.*

class MainOrderFragment : Fragment() {


    private var isNullViewRoot = false
    private var viewRoot: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isNullViewRoot = false
        if (viewRoot == null) {
            viewRoot = inflater.inflate(R.layout.fragment_order, container, false)
            isNullViewRoot = true
        }
        return viewRoot
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isNullViewRoot) {
            setGUI()
        }

    }


    override fun onResume() {
        super.onResume()
    }


    override fun onDestroy() {
        super.onDestroy()


    }

    override fun onStop() {
        super.onStop()
    }

    fun showStudentParentNames() {
        for (i in 0 until 3) {
            val studentName = SnapTextView(requireActivity())
            studentName.setTypeface(CustomTypeFace.bold)
            studentName.setSingleLine()
            studentName.textSize = (requireActivity() as BaseActivity).size!!.loginMediumTextSize
            studentName.setText(
                if (i == 0) {
                    getString(R.string.completed)
                } else if (i == 1) {
                    getString(R.string.processing_large)

                } else {
                    getString(R.string.cancel_order)
                }
            )
            if (i == 0) {
                studentName.setTextColor(ContextCompat.getColor(requireActivity(), R.color.black))
                tabLayout.addTab(tabLayout.newTab().setCustomView(studentName), true)
            } else {
                studentName.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.gray_999
                    )
                )
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
                unSelView?.setTextColor(ContextCompat.getColor(requireActivity(), R.color.gray_999))
            }
        }
        tabLayout.post { tabLayout.getTabAt(position)?.select() }
    }


    private fun setGUI() {
        setAdapter()
        showStudentParentNames()
        mPager.currentItem = 1
    }

    private fun setAdapter() {
        mPager.offscreenPageLimit = 3
        val teacherPageAdapter = TeacherPageAdapter(childFragmentManager)
        mPager.adapter = teacherPageAdapter
        tabLayout!!.setSelectedTabIndicatorColor(
            ContextCompat.getColor(
                requireActivity(),
                R.color.color_002366
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


    inner class TeacherPageAdapter constructor(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        val mPageReferenceMap = HashMap<Int, Fragment>()
        override fun getItem(position: Int): Fragment {
            val fragment = when (position) {
                0 -> CompletedFragment()
                1 -> ProcessingFragment()
                2 -> CancelledFragment()
                else -> ProcessingFragment()
            }
            mPageReferenceMap.put(position, fragment)
            return fragment
        }

        override fun getCount(): Int {
            return 3
        }

        override fun destroyItem(container: ViewGroup, position: Int, anyO: Any) {
            super.destroyItem(container, position, anyO)
            mPageReferenceMap.remove(position)
        }
    }


}