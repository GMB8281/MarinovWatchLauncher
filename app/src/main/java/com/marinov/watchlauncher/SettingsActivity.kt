package com.marinov.watchlauncher

import android.content.Intent
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<ImageView>(R.id.btnSettingsBack)
        btnBack.setOnClickListener { finish() }

        val switchDoubleTap = findViewById<SwitchCompat>(R.id.switchDoubleTap)
        val prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE)

        // Carrega estado atual
        val doubleTapEnabled = prefs.getBoolean("double_tap_lock", true)
        switchDoubleTap.isChecked = doubleTapEnabled

        switchDoubleTap.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            prefs.edit { putBoolean("double_tap_lock", isChecked) }
        }

        // Botão Ocultar Apps
        val btnHideApps = findViewById<TextView>(R.id.btnHideApps)
        btnHideApps.setOnClickListener {
            startActivity(Intent(this, HiddenAppsActivity::class.java))
        }

        // Botão Reiniciar Launcher
        val btnRestart = findViewById<TextView>(R.id.btnRestartLauncher)
        btnRestart.setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finishAffinity()
        }
    }
}