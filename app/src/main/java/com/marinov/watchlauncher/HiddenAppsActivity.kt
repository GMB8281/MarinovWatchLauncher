package com.marinov.watchlauncher

import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.edit

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
                .filter { it.activityInfo.name != "com.marinov.watchlauncher.LauncherActivity" }
                .filter { it.activityInfo.name != "com.marinov.watchlauncher.SettingsActivity" }
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

class HiddenAppsAdapter(
    private val apps: List<AppInfo>,
    private val hiddenPackages: MutableSet<String>,
    private val onToggle: (String, Boolean) -> Unit
) : RecyclerView.Adapter<HiddenAppsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgHiddenIcon)
        val txtName: TextView = view.findViewById(R.id.txtHiddenName)
        val checkBox: CheckBox = view.findViewById(R.id.checkHidden)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hidden_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.txtName.text = app.label
        holder.imgIcon.setImageDrawable(app.icon)

        val isChecked = hiddenPackages.contains(app.packageName)
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = isChecked
        holder.checkBox.setOnCheckedChangeListener { _, checked ->
            onToggle(app.packageName, checked)
        }

        holder.itemView.setOnClickListener {
            holder.checkBox.toggle()
        }
    }

    override fun getItemCount() = apps.size
}