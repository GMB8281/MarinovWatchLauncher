package com.marinov.watchlauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class LauncherActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var isAskingPermissions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = LauncherPagerAdapter(this)

        // Mantém as abas laterais na memória para transições fluidas
        viewPager.offscreenPageLimit = 2

        // Inicia na tela do Centro (Relógio)
        viewPager.setCurrentItem(1, false)

        // Manipulador do botão físico "Voltar"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                resetToClockScreen()
            }
        })
    }

    // O onResume é o melhor lugar para checar permissões em sequência,
    // pois ele roda novamente quando o usuário volta da tela de configurações.
    override fun onResume() {
        super.onResume()
        checkPermissionsSequentially()
    }

    private fun checkPermissionsSequentially() {
        if (isAskingPermissions) return

        val prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

        // 1. Checa a bateria
        if (!PermissionUtils.isBatteryOptimizationIgnored(this) && !prefs.getBoolean("ignore_battery_prompt", false)) {
            isAskingPermissions = true
            AlertDialog.Builder(this)
                .setTitle("Otimização de Bateria")
                .setMessage("Para rodar fluidamente em segundo plano, desative a otimização de bateria para a Launcher.")
                .setPositiveButton("Configurar") { _, _ ->
                    isAskingPermissions = false
                    PermissionUtils.launchBatterySettings(this)
                }
                .setNegativeButton("Agora não") { _, _ ->
                    prefs.edit().putBoolean("ignore_battery_prompt", true).apply()
                    isAskingPermissions = false
                    checkPermissionsSequentially() // Tenta a próxima
                }
                .setCancelable(false)
                .show()
            return
        }

        // 2. Checa as notificações
        if (!PermissionUtils.isNotificationAccessGranted(this) && !prefs.getBoolean("ignore_notif_prompt", false)) {
            isAskingPermissions = true
            AlertDialog.Builder(this)
                .setTitle("Acesso a Notificações")
                .setMessage("Para exibir suas mensagens, precisamos de permissão para ler as notificações do sistema.")
                .setPositiveButton("Permitir") { _, _ ->
                    isAskingPermissions = false
                    PermissionUtils.launchNotificationSettings(this)
                }
                .setNegativeButton("Agora não") { _, _ ->
                    prefs.edit().putBoolean("ignore_notif_prompt", true).apply()
                    isAskingPermissions = false
                }
                .setCancelable(false)
                .show()
            return
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            resetToClockScreen()
        }
    }

    private fun resetToClockScreen(): Boolean {
        var navigated = false

        // Resolvido o bug que você comentou: envolver em try-catch evita crash se a view ainda não existir.
        // 1. Volta o paginador de widgets para a primeira página
        try {
            val widgetsViewPager = findViewById<ViewPager2>(R.id.viewPagerWidgets)
            if (widgetsViewPager != null && widgetsViewPager.currentItem != 0) {
                widgetsViewPager.setCurrentItem(0, true)
                navigated = true
            }
        } catch (e: Exception) { e.printStackTrace() }

        // 2. Volta o paginador vertical (Desce da tela de Widgets para o Relógio)
        try {
            val verticalViewPager = findViewById<ViewPager2>(R.id.viewPagerVertical)
            if (verticalViewPager != null && verticalViewPager.currentItem != 0) {
                verticalViewPager.setCurrentItem(0, true)
                navigated = true
            }
        } catch (e: Exception) { e.printStackTrace() }

        // 3. Volta o paginador horizontal
        if (viewPager.currentItem != 1) {
            viewPager.setCurrentItem(1, true)
            navigated = true
        }

        return navigated
    }
}