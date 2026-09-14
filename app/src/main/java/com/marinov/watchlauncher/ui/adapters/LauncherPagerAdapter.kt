package com.marinov.watchlauncher.ui.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.marinov.watchlauncher.ui.fragments.AppDrawerFragment
import com.marinov.watchlauncher.ui.fragments.CenterFragment
import com.marinov.watchlauncher.ui.fragments.NotificationFragment

class LauncherPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NotificationFragment()
            1 -> CenterFragment()
            2 -> AppDrawerFragment()
            else -> CenterFragment()
        }
    }
}