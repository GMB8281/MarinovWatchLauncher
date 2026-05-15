package com.marinov.watchlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val apps: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private var onLongClickListener: ((AppInfo, View) -> Unit)? = null

    fun setOnLongClickListener(listener: (AppInfo, View) -> Unit) {
        this.onLongClickListener = listener
    }

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgAppIcon)
        val txtName: TextView = view.findViewById(R.id.txtAppName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]

        holder.txtName.text = app.label
        holder.imgIcon.setImageDrawable(app.icon)

        holder.itemView.setOnClickListener { onClick(app) }

        holder.itemView.setOnLongClickListener {
            onLongClickListener?.invoke(app, it)
            true
        }
    }

    override fun getItemCount() = apps.size
}