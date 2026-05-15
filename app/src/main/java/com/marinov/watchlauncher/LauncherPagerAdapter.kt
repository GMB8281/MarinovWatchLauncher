package com.marinov.watchlauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

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