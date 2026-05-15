package com.marinov.watchlauncher.widgets

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.marinov.watchlauncher.R
import kotlin.math.min

class WidgetFragment : Fragment() {

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    private val APPWIDGET_HOST_ID = 1024
    private val REQUEST_PICK_APPWIDGET = 1
    private val REQUEST_CREATE_APPWIDGET = 2

    private lateinit var viewPagerWidgets: ViewPager2
    private var widgetIds = mutableListOf<Int>()
    private lateinit var adapter: WidgetPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_widget, container, false)
        viewPagerWidgets = view.findViewById(R.id.viewPagerWidgets)

        appWidgetManager = AppWidgetManager.getInstance(requireContext())
        appWidgetHost = AppWidgetHost(requireContext(), APPWIDGET_HOST_ID)
        appWidgetHost.startListening()

        // Carrega a lista de widgets salvos no dispositivo
        loadSavedWidgets()

        adapter = WidgetPagerAdapter()
        viewPagerWidgets.adapter = adapter
        viewPagerWidgets.offscreenPageLimit = 5 // Mantém os 5 widgets rodando fluidamente na memória

        return view
    }

    private fun loadSavedWidgets() {
        val prefs = requireContext().getSharedPreferences("watch_launcher_prefs", Context.MODE_PRIVATE)
        val idsStr = prefs.getString("widget_ids", "") ?: ""
        widgetIds = if (idsStr.isEmpty()) {
            mutableListOf()
        } else {
            idsStr.split(",").map { it.toInt() }.toMutableList()
        }
    }

    private fun saveWidgets() {
        val prefs = requireContext().getSharedPreferences("watch_launcher_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("widget_ids", widgetIds.joinToString(",")).apply()
    }

    private fun selectWidget() {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_PICK_APPWIDGET -> configureWidget(data)
                REQUEST_CREATE_APPWIDGET -> finalizeWidgetCreation(data)
            }
        } else if (requestCode == REQUEST_PICK_APPWIDGET && data != null) {
            val appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            if (appWidgetId != -1) {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }
    }

    private fun configureWidget(data: Intent?) {
        val extras = data?.extras
        val appWidgetId = extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)

        if (appWidgetInfo != null && appWidgetInfo.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            intent.component = appWidgetInfo.configure
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET)
        } else {
            finalizeWidgetCreation(data)
        }
    }

    private fun finalizeWidgetCreation(data: Intent?) {
        val extras = data?.extras
        val appWidgetId = extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1

        if (appWidgetId != -1) {
            widgetIds.add(appWidgetId)
            saveWidgets()

            // Atualiza a tela de widgets nativamente e move o paginador para mostrar o novo widget
            adapter.notifyDataSetChanged()
            viewPagerWidgets.currentItem = widgetIds.size - 1
            Toast.makeText(requireContext(), "Widget adicionado!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptRemoveWidget(position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remover Widget?")
            .setPositiveButton("Sim") { _, _ ->
                val idToRemove = widgetIds.removeAt(position)
                appWidgetHost.deleteAppWidgetId(idToRemove)
                saveWidgets()
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Não", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appWidgetHost.stopListening()
    }

    // --- Adapter Interno para gerenciar as páginas de Widgets ---
    inner class WidgetPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_ADD = 0
        private val TYPE_WIDGET = 1

        override fun getItemViewType(position: Int): Int {
            // Se a posição for igual ao tamanho da lista atual, é o botão de adicionar.
            // Exemplo: 2 widgets = posições 0 e 1. Se a posição for 2, é o botão +.
            return if (position == widgetIds.size) TYPE_ADD else TYPE_WIDGET
        }

        override fun getItemCount(): Int {
            // Conta = widgets instalados + 1 botão adicionar. O limite máximo absoluto é 5 telas.
            return min(widgetIds.size + 1, 5)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_ADD) {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_widget_add, parent, false)
                AddViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_widget_host, parent, false)
                WidgetViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is AddViewHolder) {
                holder.fabAdd.setOnClickListener { selectWidget() }
            } else if (holder is WidgetViewHolder) {
                val appWidgetId = widgetIds[position]
                val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)

                holder.container.removeAllViews()

                if (appWidgetInfo != null) {
                    val hostView = appWidgetHost.createView(requireContext(), appWidgetId, appWidgetInfo)
                    hostView.setAppWidget(appWidgetId, appWidgetInfo)

                    // Previne que alguns widgets "roubem" o clique longo do container
                    hostView.setOnLongClickListener {
                        promptRemoveWidget(position)
                        true
                    }
                    holder.container.addView(hostView)
                }

                // Permite deletar segurando na área vazia da tela do widget também
                holder.container.setOnLongClickListener {
                    promptRemoveWidget(position)
                    true
                }
            }
        }

        inner class AddViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val fabAdd: FloatingActionButton = view.findViewById(R.id.fabAddWidget)
        }

        inner class WidgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val container: FrameLayout = view.findViewById(R.id.widgetHostContainer)
        }
    }
}