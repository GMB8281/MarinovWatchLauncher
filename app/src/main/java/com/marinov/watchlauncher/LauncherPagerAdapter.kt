package com.marinov.watchlauncher.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.marinov.watchlauncher.CenterFragment
import com.marinov.watchlauncher.apps.AppDrawerFragment
import com.marinov.watchlauncher.notifications.NotificationFragment

class LauncherPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NotificationFragment() // Esquerda
            1 -> CenterFragment()       // Centro (Substituído: Contém Relógio e Widgets na vertical)
            2 -> AppDrawerFragment()    // Direita
            else -> CenterFragment()
        }
    }
}