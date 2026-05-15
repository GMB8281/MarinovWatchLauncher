package com.marinov.watchlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.marinov.watchlauncher.adapters.LauncherPagerAdapter
import com.marinov.watchlauncher.utils.PermissionUtils
import kotlinx.coroutines.delay

class LauncherActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        // Solicita permissões necessárias na inicialização
        PermissionUtils.requestBatteryOptimization(this)
        PermissionUtils.requestNotificationAccess(this)

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = LauncherPagerAdapter(this)

        // Mantém as abas laterais na memória para transições fluidas
        viewPager.offscreenPageLimit = 2

        // Inicia na tela do Centro (Relógio)
        viewPager.setCurrentItem(1, false)

        // Manipulador do botão físico "Voltar" (Substitui o onBackPressed depreciado)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {// Tenta voltar para o relógio. Se já estiver no relógio, engole o clique (comportamento de Launcher)
                resetToClockScreen()
            }
        })
    }

    // Manipulador do botão físico "Home" (Acionado pelo sistema Android)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            resetToClockScreen()
        }
    }

    private fun resetToClockScreen(): Boolean {
        var navigated = false
// resolver bug antes de implementar
        // 1. Volta o paginador de widgets para a primeira página (se existir na tela)
       // val widgetsViewPager = findViewById<ViewPager2>(R.id.viewPagerWidgets)
       // if (widgetsViewPager != null && widgetsViewPager.currentItem != 0) {
        //    widgetsViewPager.setCurrentItem(0, true)
        //    navigated = true
       // }

        // 2. Volta o paginador vertical (Desce da tela de Widgets para o Relógio)
        val verticalViewPager = findViewById<ViewPager2>(R.id.viewPagerVertical)
        if (verticalViewPager != null && verticalViewPager.currentItem != 0) {
            verticalViewPager.setCurrentItem(0, true)
            navigated = true
        }

        // 3. Volta o paginador horizontal (Sai de Notificações ou Apps e vai para o Centro)
        if (viewPager.currentItem != 1) {
            viewPager.setCurrentItem(1, true)
            navigated = true
        }

        return navigated
    }
}