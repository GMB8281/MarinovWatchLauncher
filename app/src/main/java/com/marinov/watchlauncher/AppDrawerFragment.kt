package com.marinov.watchlauncher.apps

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.marinov.watchlauncher.R

data class AppInfo(val label: String, val icon: Drawable, val packageName: String)

class AppDrawerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_apps, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewApps)

        // Grade de 2 colunas
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val apps = loadApps()
        recyclerView.adapter = AppAdapter(apps) { appInfo ->
            val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        }

        return view
    }

    private fun loadApps(): List<AppInfo> {
        val pm = requireContext().packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)

        return resolveInfos.map {
            AppInfo(
                label = it.loadLabel(pm).toString(),
                icon = it.loadIcon(pm),
                packageName = it.activityInfo.packageName
            )
        }.sortedBy { it.label.lowercase() } // Ordem alfabética
    }
}