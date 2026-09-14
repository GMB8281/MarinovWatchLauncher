package com.marinov.watchlauncher.ui.activities

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.marinov.watchlauncher.R
import com.marinov.watchlauncher.receivers.LockScreenReceiver
import com.marinov.watchlauncher.ui.adapters.LauncherPagerAdapter
import com.marinov.watchlauncher.utils.PermissionUtils

class LauncherActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var isAskingPermissions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = LauncherPagerAdapter(this)
        viewPager.offscreenPageLimit = 2
        viewPager.setCurrentItem(1, false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                resetToClockScreen()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsSequentially()
    }

    private fun checkPermissionsSequentially() {
        if (isAskingPermissions) return
        val prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

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
                    checkPermissionsSequentially()
                }
                .setCancelable(false)
                .show()
            return
        }

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

        val doubleTapEnabled = prefs.getBoolean("double_tap_lock", true)
        if (doubleTapEnabled && !isDeviceAdminActive() && !prefs.getBoolean("ignore_admin_prompt", false)) {
            isAskingPermissions = true
            AlertDialog.Builder(this)
                .setTitle("Permissão de Administrador")
                .setMessage("Para bloquear a tela com duplo toque, é necessário ativar o administrador do dispositivo.")
                .setPositiveButton("Ativar") { _, _ ->
                    isAskingPermissions = false
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdminComponentName())
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Permita que o launcher bloqueie a tela automaticamente.")
                    startActivity(intent)
                }
                .setNegativeButton("Agora não") { _, _ ->
                    prefs.edit().putBoolean("ignore_admin_prompt", true).apply()
                    isAskingPermissions = false
                }
                .setCancelable(false)
                .show()
        }
    }

    fun isDeviceAdminActive(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(getAdminComponentName())
    }

    fun getAdminComponentName(): ComponentName {
        return ComponentName(this, LockScreenReceiver::class.java)
    }

    fun lockScreen() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        dpm.lockNow()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            resetToClockScreen()
        }
    }

    private fun resetToClockScreen(): Boolean {
        var navigated = false
        try {
            val widgetsViewPager = findViewById<ViewPager2>(R.id.viewPagerWidgets)
            if (widgetsViewPager != null && widgetsViewPager.currentItem != 0) {
                widgetsViewPager.setCurrentItem(0, true)
                navigated = true
            }
        } catch (_: Exception) {}

        try {
            val verticalViewPager = findViewById<ViewPager2>(R.id.viewPagerVertical)
            if (verticalViewPager != null && verticalViewPager.currentItem != 0) {
                verticalViewPager.setCurrentItem(0, true)
                navigated = true
            }
        } catch (_: Exception) {}

        if (viewPager.currentItem != 1) {
            viewPager.setCurrentItem(1, true)
            navigated = true
        }
        return navigated
    }
}