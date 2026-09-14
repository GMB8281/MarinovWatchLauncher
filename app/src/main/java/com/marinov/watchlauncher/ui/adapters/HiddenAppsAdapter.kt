package com.marinov.watchlauncher.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.marinov.watchlauncher.R
import com.marinov.watchlauncher.data.model.AppInfo

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