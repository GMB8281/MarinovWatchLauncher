package com.marinov.watchlauncher

import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class NotificationFragment : Fragment() {

    private var currentNotifIndex = 0
    private var notifications = listOf<StatusBarNotification>()

    private lateinit var cardNotification: CardView
    private lateinit var txtNoNotifications: TextView
    private lateinit var txtTitle: TextView
    private lateinit var txtText: TextView
    private lateinit var txtAppName: TextView
    private lateinit var imgIcon: ImageView
    private lateinit var btnClear: MaterialButton
    private lateinit var btnOpen: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        cardNotification = view.findViewById(R.id.cardNotification)
        txtNoNotifications = view.findViewById(R.id.txtNoNotifications)
        txtTitle = view.findViewById(R.id.txtNotifTitle)
        txtText = view.findViewById(R.id.txtNotifText)
        txtAppName = view.findViewById(R.id.txtNotifAppName)
        imgIcon = view.findViewById(R.id.imgNotifIcon)
        btnClear = view.findViewById(R.id.btnNotifClear)
        btnOpen = view.findViewById(R.id.btnNotifOpen)

        WatchNotificationListener.notificationsLiveData.observe(viewLifecycleOwner) { notifs ->
            notifications = notifs
            if (currentNotifIndex >= notifications.size) {
                currentNotifIndex = 0
            }
            updateUI()
        }

        btnClear.setOnClickListener {
            if (notifications.isNotEmpty()) {
                val sbn = notifications[currentNotifIndex]
                WatchNotificationListener.instance?.clearNotification(sbn.key)
                // O listener irá atualizar a lista e o observe() atualizará a UI com a próxima
            }
        }

        btnOpen.setOnClickListener {
            if (notifications.isNotEmpty()) {
                val sbn = notifications[currentNotifIndex]
                try {
                    sbn.notification.contentIntent?.send()
                } catch (e: PendingIntent.CanceledException) {
                    e.printStackTrace()
                }
            }
        }

        return view
    }

    private fun updateUI() {
        if (notifications.isEmpty()) {
            cardNotification.visibility = View.GONE
            txtNoNotifications.visibility = View.VISIBLE
        } else {
            cardNotification.visibility = View.VISIBLE
            txtNoNotifications.visibility = View.GONE

            val sbn = notifications[currentNotifIndex]
            val extras = sbn.notification.extras

            val title = extras.getString("android.title") ?: "Sem Título"
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            txtTitle.text = title
            txtText.text = text

            try {
                val pm = requireContext().packageManager
                val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
                txtAppName.text = pm.getApplicationLabel(appInfo)
                imgIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
            } catch (_: PackageManager.NameNotFoundException) {
                txtAppName.text = sbn.packageName
            }
        }
    }
}