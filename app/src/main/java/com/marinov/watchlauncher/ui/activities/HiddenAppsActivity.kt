package com.marinov.watchlauncher.ui.activities

import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.marinov.watchlauncher.R
import com.marinov.watchlauncher.data.model.AppInfo
import com.marinov.watchlauncher.ui.adapters.HiddenAppsAdapter

class HiddenAppsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HiddenAppsAdapter
    private val apps = mutableListOf<AppInfo>()
    private val hiddenPackages = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hidden_apps)

        recyclerView = findViewById(R.id.recyclerViewHiddenApps)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadApps()
        loadHiddenApps()

        adapter = HiddenAppsAdapter(apps, hiddenPackages) { packageName, isChecked ->
            if (isChecked) hiddenPackages.add(packageName)
            else hiddenPackages.remove(packageName)
            saveHiddenApps()
        }
        recyclerView.adapter = adapter

        findViewById<ImageView>(R.id.btnBackHidden).setOnClickListener {
            finish()
        }
    }

    private fun loadApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)

        apps.clear()
        apps.addAll(
            resolveInfos
                .filter { it.activityInfo.name != "com.marinov.watchlauncher.ui.activities.LauncherActivity" }
                .filter { it.activityInfo.name != "com.marinov.watchlauncher.ui.activities.SettingsActivity" }
                .map {
                    AppInfo(
                        label = it.loadLabel(pm).toString(),
                        icon = it.activityInfo.loadIcon(pm),
                        packageName = it.activityInfo.packageName,
                        className = it.activityInfo.name
                    )
                }.sortedBy { it.label.lowercase() }
        )
    }

    private fun loadHiddenApps() {
        val prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE)
        hiddenPackages.clear()
        hiddenPackages.addAll(prefs.getStringSet("hidden_packages", emptySet()) ?: emptySet())
    }

    private fun saveHiddenApps() {
        val prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE)
        prefs.edit { putStringSet("hidden_packages", hiddenPackages) }
    }
}