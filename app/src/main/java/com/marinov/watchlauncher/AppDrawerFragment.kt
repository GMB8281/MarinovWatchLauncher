package com.marinov.watchlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class AppInfo(
    val label: String,
    val icon: Drawable,
    val packageName: String,
    val className: String
)

class AppDrawerFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppAdapter
    private var apps = mutableListOf<AppInfo>()

    private val SETTINGS_PACKAGE = "com.marinov.watchlauncher"
    private val SETTINGS_CLASS = "com.marinov.watchlauncher.SettingsActivity"

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_CHANGED -> loadApps()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_apps, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewApps)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = AppAdapter(apps) { app -> launchApp(app) }

        adapter.setOnLongClickListener { appInfo, anchorView ->
            showAppOptionsMenu(appInfo, anchorView)
        }

        recyclerView.adapter = adapter
        loadApps()
        return view
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        requireContext().registerReceiver(packageReceiver, filter)
        loadApps()
    }

    override fun onPause() {
        super.onPause()
        try {
            requireContext().unregisterReceiver(packageReceiver)
        } catch (_: Exception) {}
    }

    private fun loadApps() {
        val pm = requireContext().packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)

        // Carrega lista de pacotes ocultos
        val prefs = requireContext().getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        val hiddenPackages = prefs.getStringSet("hidden_packages", emptySet()) ?: emptySet()

        apps.clear()
        apps.addAll(
            resolveInfos
                .filter { it.activityInfo.name != "com.marinov.watchlauncher.LauncherActivity" }
                .filter { it.activityInfo.packageName !in hiddenPackages }
                .map {
                    AppInfo(
                        label = it.loadLabel(pm).toString(),
                        icon = it.activityInfo.loadIcon(pm),
                        packageName = it.activityInfo.packageName,
                        className = it.activityInfo.name
                    )
                }.sortedBy { it.label.lowercase() }
        )

        adapter.notifyDataSetChanged()
    }

    private fun launchApp(app: AppInfo) {
        try {
            if (app.packageName == SETTINGS_PACKAGE && app.className.contains("SettingsActivity")) {
                val intent = Intent(requireContext(), SettingsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                return
            }

            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(app.packageName, app.className)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val fallback = requireContext().packageManager.getLaunchIntentForPackage(app.packageName)
                fallback?.let { startActivity(it) }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun showAppOptionsMenu(app: AppInfo, anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)

        addShortcutsToMenu(popup, app)

        popup.menu.add("Abrir ${app.label}").setOnMenuItemClickListener {
            launchApp(app)
            true
        }

        if (app.className != SETTINGS_CLASS) {
            popup.menu.add("Informações do app").setOnMenuItemClickListener {
                showAppInfo(app.packageName)
                true
            }

            if (canBeUninstalled(app.packageName)) {
                popup.menu.add("Desinstalar").setOnMenuItemClickListener {
                    uninstallApp(app.packageName)
                    true
                }
            }
        }

        popup.show()
    }

    private fun canBeUninstalled(packageName: String): Boolean {
        return try {
            val pm = requireContext().packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            !isSystemApp || isUpdatedSystemApp
        } catch (_: Exception) {
            false
        }
    }

    private fun addShortcutsToMenu(popup: PopupMenu, app: AppInfo) {
        try {
            val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

            val shortcutQuery = LauncherApps.ShortcutQuery().apply {
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
                setPackage(app.packageName)
            }

            val shortcuts = launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle()) ?: emptyList()

            shortcuts.forEach { shortcut ->
                popup.menu.add(shortcut.shortLabel.toString()).setOnMenuItemClickListener {
                    try {
                        launcherApps.startShortcut(
                            app.packageName,
                            shortcut.id,
                            null,
                            null,
                            Process.myUserHandle()
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
            }
        } catch (_: Exception) {}
    }

    private fun showAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = "package:$packageName".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val fallback = Intent("android.intent.action.UNINSTALL_PACKAGE").apply {
                    data = "package:$packageName".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(fallback)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}