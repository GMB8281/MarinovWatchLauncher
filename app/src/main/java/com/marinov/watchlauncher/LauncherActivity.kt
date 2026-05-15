package com.marinov.watchlauncher

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.marinov.watchlauncher.adapters.LauncherPagerAdapter
import com.marinov.watchlauncher.utils.PermissionUtils

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        // Solicita permissões necessárias
        PermissionUtils.requestBatteryOptimization(this)
        PermissionUtils.requestNotificationAccess(this)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)

        // Configura o ViewPager com as 3 telas principais
        viewPager.adapter = LauncherPagerAdapter(this)
        // Define o Relógio (posição 1) como tela inicial. 0 = Notificações, 2 = Apps
        viewPager.setCurrentItem(1, false)
        viewPager.offscreenPageLimit = 2 // Mantém tudo carregado na memória para transições suaves
    }

    // Desativa o botão voltar na tela principal
    @SuppressLint("GestureBackNavigation", "MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        if (viewPager.currentItem != 1) {
            viewPager.setCurrentItem(1, true)
        }
    }
}