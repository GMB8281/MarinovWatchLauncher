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
    val packageName: String
)

class AppDrawerFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppAdapter
    private var apps = mutableListOf<AppInfo>()

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

        // A primeira carga acontece aqui
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

        // CORREÇÃO: Força a atualização da lista sempre que a tela de apps reaparece.
        // Resolve o problema de quando o usuário instala um app fora da Launcher (ex: via PlayStore).
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

        apps.clear()
        apps.addAll(
            resolveInfos.map {
                AppInfo(
                    label = it.loadLabel(pm).toString(),
                    icon = it.loadIcon(pm),
                    packageName = it.activityInfo.packageName
                )
            }.sortedBy { it.label.lowercase() }
        )

        adapter.notifyDataSetChanged()
    }

    private fun launchApp(app: AppInfo) {
        val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(app.packageName)
        launchIntent?.let { startActivity(it) }
    }

    // ==================== MENU AO SEGURAR ====================
    private fun showAppOptionsMenu(app: AppInfo, anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)

        // 1. Quick Shortcuts (se disponíveis)
        addShortcutsToMenu(popup, app)

        // 2. Abrir o app
        popup.menu.add("Abrir ${app.label}").setOnMenuItemClickListener {
            launchApp(app)
            true
        }

        // 3. Informações do App
        popup.menu.add("Informações do app").setOnMenuItemClickListener {
            showAppInfo(app.packageName)
            true
        }

        // 4. Desinstalar (só para apps removíveis)
        if (canBeUninstalled(app.packageName)) {
            popup.menu.add("Desinstalar").setOnMenuItemClickListener {
                uninstallApp(app.packageName)
                true
            }
        }

        popup.show()
    }

    private fun canBeUninstalled(packageName: String): Boolean {
        return try {
            val pm = requireContext().packageManager
            val appInfo: ApplicationInfo = pm.getApplicationInfo(packageName, 0)

            // Apps de sistema puro geralmente não podem ser desinstalados
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            // Pode desinstalar se NÃO for app de sistema OU for um sistema atualizado
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
        } catch (_: Exception) {
            // Falha silenciosa
        }
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
                val fallbackIntent = Intent("android.intent.action.UNINSTALL_PACKAGE").apply {
                    data = "package:$packageName".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(fallbackIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}