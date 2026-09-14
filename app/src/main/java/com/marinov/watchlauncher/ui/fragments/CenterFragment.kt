package com.marinov.watchlauncher.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.marinov.watchlauncher.R

class CenterFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_center, container, false)
        val viewPager: ViewPager2 = view.findViewById(R.id.viewPagerVertical)

        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val parentViewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
                parentViewPager?.isUserInputEnabled = (position == 0)
            }
        })

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> ClockFragment()
                    1 -> WidgetFragment()
                    else -> ClockFragment()
                }
            }
        }
        return view
    }
}