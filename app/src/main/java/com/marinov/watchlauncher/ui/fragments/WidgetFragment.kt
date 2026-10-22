package com.marinov.watchlauncher.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.marinov.watchlauncher.R

class WidgetFragment : Fragment() {

    companion object {
        private const val APPWIDGET_HOST_ID = 1024
        private const val REQUEST_PICK_APPWIDGET = 1
        private const val REQUEST_CREATE_APPWIDGET = 2

        private const val PREFS_NAME = "watch_launcher_prefs"
        private const val KEY_WIDGET_IDS = "widget_ids"
        private const val MAX_WIDGETS = 5
    }

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var viewPagerWidgets: ViewPager2
    private lateinit var adapter: WidgetPagerAdapter

    private val widgetIds = mutableListOf<Int>()
    private var receiverRegistered = false

    /**
     * Mantém a lista de widgets coerente quando um aplicativo é desinstalado,
     * atualizado ou tem seus dados limpos, mesmo com a tela aberta.
     */
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_FULLY_REMOVED,
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_DATA_CLEARED -> removeInvalidWidgets()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_widget, container, false)

        viewPagerWidgets = view.findViewById(R.id.viewPagerWidgets)
        appWidgetManager = AppWidgetManager.getInstance(requireContext())

        // applicationContext evita que o AppCompat substitua as Views internas
        // dos RemoteViews (ImageView -> AppCompatImageView, etc).
        appWidgetHost = AppWidgetHost(
            requireContext().applicationContext,
            APPWIDGET_HOST_ID
        )
        appWidgetHost.startListening()

        // Já filtra os IDs órfãos antes de montar o adapter.
        loadSavedWidgets()

        adapter = WidgetPagerAdapter()
        viewPagerWidgets.adapter = adapter
        viewPagerWidgets.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT

        return view
    }

    override fun onResume() {
        super.onResume()

        // Cobre o caso em que o usuário foi desinstalar o app e voltou ao launcher.
        if (::adapter.isInitialized) {
            removeInvalidWidgets()
        }

        registerPackageReceiver()
    }

    override fun onPause() {
        unregisterPackageReceiver()
        super.onPause()
    }

    override fun onDestroyView() {
        unregisterPackageReceiver()

        if (::appWidgetHost.isInitialized) {
            try {
                appWidgetHost.stopListening()
            } catch (_: Exception) {
            }
        }

        if (::viewPagerWidgets.isInitialized) {
            viewPagerWidgets.adapter = null
        }

        super.onDestroyView()
    }

    // ---------------------------------------------------------------------
    // Broadcast de pacotes
    // ---------------------------------------------------------------------

    private fun registerPackageReceiver() {
        if (receiverRegistered) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_DATA_CLEARED)
            addDataScheme("package")
        }

        try {
            requireContext().registerReceiver(packageReceiver, filter)
            receiverRegistered = true
        } catch (_: Exception) {
        }
    }

    private fun unregisterPackageReceiver() {
        if (!receiverRegistered) return

        try {
            context?.unregisterReceiver(packageReceiver)
        } catch (_: Exception) {
        }

        receiverRegistered = false
    }

    // ---------------------------------------------------------------------
    // Persistência
    // ---------------------------------------------------------------------

    private fun loadSavedWidgets() {
        val prefs =
            requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val stored = prefs.getString(KEY_WIDGET_IDS, "").orEmpty()

        widgetIds.clear()

        stored.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
            .forEach { id ->
                // Só mantém o ID se o widget ainda existe e o app continua instalado.
                if (widgetIds.size < MAX_WIDGETS && isWidgetValid(id)) {
                    widgetIds.add(id)
                }
            }

        // Regravamos a lista limpa, eliminando de vez os IDs órfãos.
        saveWidgets()
    }

    private fun saveWidgets() {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        prefs.edit {
            putString(KEY_WIDGET_IDS, widgetIds.joinToString(","))
        }
    }

    // ---------------------------------------------------------------------
    // VALIDAÇÃO DOS WIDGETS (correção do bug)
    // ---------------------------------------------------------------------

    /**
     * Um widget só é válido se:
     *  1) O AppWidgetManager ainda possui o registro desse appWidgetId;
     *  2) O pacote do provedor continua instalado e habilitado.
     *
     * Se o app foi desinstalado, uma dessas duas condições falha.
     */
    private fun isWidgetValid(appWidgetId: Int): Boolean {
        val ctx = context ?: return false
        return appWidgetId > 0 && try {
            val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
            val provider = info?.provider

            if (provider == null) {
                false
            } else {
                val pm = ctx.packageManager
                val appInfo = pm.getApplicationInfo(provider.packageName, 0)
                appInfo.enabled
            }
        } catch (_: Exception) {
            // NameNotFoundException (app desinstalado) ou ID já liberado pelo sistema.
            false
        }
    }

    /**
     * Remove da lista e das preferências todos os widgets cujo aplicativo
     * não existe mais. Retorna true se algo foi removido.
     */
    private fun removeInvalidWidgets(): Boolean {
        if (!::appWidgetManager.isInitialized) return false
        if (widgetIds.isEmpty()) return false

        val invalid = widgetIds.filter { !isWidgetValid(it) }
        if (invalid.isEmpty()) return false

        widgetIds.removeAll(invalid)

        // Não é necessário chamar deleteAppWidgetId aqui: ao desinstalar o pacote,
        // o próprio sistema já libera os IDs. Removemos apenas a referência órfã.
        saveWidgets()
        refreshWidgets()

        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshWidgets() {
        if (!::adapter.isInitialized || !::viewPagerWidgets.isInitialized) return

        // A última página é sempre a página "+ adicionar".
        val maxPosition = widgetIds.size

        if (viewPagerWidgets.currentItem > maxPosition) {
            viewPagerWidgets.setCurrentItem(maxPosition, false)
        }

        adapter.notifyDataSetChanged()
    }

    // ---------------------------------------------------------------------
    // Adicionar widget
    // ---------------------------------------------------------------------

    private fun selectWidget() {
        if (widgetIds.size >= MAX_WIDGETS) {
            showToast("Limite de $MAX_WIDGETS widgets atingido.")
            return
        }

        val appWidgetId = appWidgetHost.allocateAppWidgetId()

        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        try {
            startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET)
        } catch (_: Exception) {
            releaseWidgetId(appWidgetId)
            showToast("Não foi possível abrir a lista de widgets.")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        val selectedId =
            data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1

        when (requestCode) {
            REQUEST_PICK_APPWIDGET -> {
                if (resultCode == Activity.RESULT_OK) {
                    configureWidget(data)
                } else if (selectedId != -1) {
                    // Usuário cancelou a seleção: não deixa ID fantasma no host.
                    releaseWidgetId(selectedId)
                }
            }

            REQUEST_CREATE_APPWIDGET -> {
                if (resultCode == Activity.RESULT_OK) {
                    finalizeWidgetCreation(data)
                } else if (selectedId != -1) {
                    releaseWidgetId(selectedId)
                }
            }
        }
    }

    private fun configureWidget(data: Intent?) {
        val appWidgetId =
            data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1

        if (appWidgetId == -1) {
            finalizeWidgetCreation(data)
            return
        }

        val info = try {
            appWidgetManager.getAppWidgetInfo(appWidgetId)
        } catch (_: Exception) {
            null
        }

        if (info?.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = info.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }

            try {
                startActivityForResult(intent, REQUEST_CREATE_APPWIDGET)
            } catch (_: Exception) {
                finalizeWidgetCreation(data)
            }
        } else {
            finalizeWidgetCreation(data)
        }
    }

    private fun finalizeWidgetCreation(data: Intent?) {
        val appWidgetId =
            data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1

        if (appWidgetId <= 0 || widgetIds.contains(appWidgetId)) return

        if (widgetIds.size >= MAX_WIDGETS) {
            releaseWidgetId(appWidgetId)
            showToast("Limite de $MAX_WIDGETS widgets atingido.")
            return
        }

        // Protege também contra o app ser desinstalado durante a configuração.
        if (!isWidgetValid(appWidgetId)) {
            releaseWidgetId(appWidgetId)
            showToast("Widget indisponível: o aplicativo pode ter sido desinstalado.")
            return
        }

        widgetIds.add(appWidgetId)
        saveWidgets()
        refreshWidgets()

        val newPosition = widgetIds.size - 1
        viewPagerWidgets.post {
            if (isAdded && ::viewPagerWidgets.isInitialized) {
                viewPagerWidgets.setCurrentItem(newPosition, false)
            }
        }

        showToast("Widget adicionado!")
    }

    private fun releaseWidgetId(appWidgetId: Int) {
        if (appWidgetId <= 0) return

        try {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        } catch (_: Exception) {
        }

        // Algumas ROMs desativam o listener ao excluir um ID; garantimos que
        // o host continue recebendo atualizações dos demais widgets.
        if (::appWidgetHost.isInitialized) {
            try {
                appWidgetHost.startListening()
            } catch (_: Exception) {
            }
        }
    }

    // ---------------------------------------------------------------------
    // Remover widget
    // ---------------------------------------------------------------------

    private fun promptRemoveWidget(position: Int) {
        // Usa o ID, e não a posição, porque o position pode estar obsoleto
        // depois de o RecyclerView reciclar o ViewHolder.
        val appWidgetId = widgetIds.getOrNull(position) ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Remover Widget?")
            .setPositiveButton("Sim") { _, _ ->
                val index = widgetIds.indexOf(appWidgetId)
                if (index < 0) return@setPositiveButton

                widgetIds.removeAt(index)
                releaseWidgetId(appWidgetId)
                saveWidgets()
                refreshWidgets()
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun showToast(message: String) {
        val ctx = context ?: return
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
    }

    // ---------------------------------------------------------------------
    // Adapter
    // ---------------------------------------------------------------------

    private fun bindWidget(holder: WidgetViewHolder, position: Int) {
        holder.container.removeAllViews()

        val appWidgetId = widgetIds.getOrNull(position) ?: return

        val info = try {
            appWidgetManager.getAppWidgetInfo(appWidgetId)
        } catch (_: Exception) {
            null
        }

        if (info == null || !isWidgetValid(appWidgetId)) {
            // App desinstalado / provedor inexistente:
            // não deixa a página em branco e remove a referência órfã.
            viewPagerWidgets.post {
                if (isAdded) removeInvalidWidgets()
            }
            return
        }

        val hostView = try {
            appWidgetHost.createView(
                requireContext().applicationContext,
                appWidgetId,
                info
            )
        } catch (_: Exception) {
            null
        } ?: return

        hostView.setAppWidget(appWidgetId, info)

        hostView.setOnLongClickListener {
            promptRemoveWidget(position)
            true
        }

        holder.container.setOnLongClickListener {
            promptRemoveWidget(position)
            true
        }

        holder.container.addView(hostView)
    }

    inner class WidgetPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_ADD = 0
        private val TYPE_WIDGET = 1

        // Sempre existe a página "+", inclusive quando não há nenhum widget.
        override fun getItemCount(): Int = widgetIds.size + 1

        override fun getItemViewType(position: Int): Int {
            return if (position >= widgetIds.size) TYPE_ADD else TYPE_WIDGET
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            val layout = if (viewType == TYPE_ADD) {
                R.layout.item_widget_add
            } else {
                R.layout.item_widget_host
            }

            val view = LayoutInflater.from(parent.context)
                .inflate(layout, parent, false)

            return if (viewType == TYPE_ADD) {
                AddViewHolder(view)
            } else {
                WidgetViewHolder(view)
            }
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int
        ) {
            when (holder) {
                is AddViewHolder -> {
                    holder.fabAdd.setOnClickListener { selectWidget() }
                }

                is WidgetViewHolder -> {
                    bindWidget(holder, position)
                }
            }
        }
    }

    class AddViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fabAdd: FloatingActionButton = view.findViewById(R.id.fabAddWidget)
    }

    class WidgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: FrameLayout = view.findViewById(R.id.widgetHostContainer)
    }
}