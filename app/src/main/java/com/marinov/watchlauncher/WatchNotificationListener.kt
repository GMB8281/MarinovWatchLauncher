package com.marinov.watchlauncher

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.lifecycle.MutableLiveData

class WatchNotificationListener : NotificationListenerService() {

    companion object {
        // LiveData para observar notificações no Fragment renomeado para evitar conflitos
        val notificationsLiveData = MutableLiveData<List<StatusBarNotification>>()
        var instance: WatchNotificationListener? = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        updateNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateNotifications()
    }

    private fun updateNotifications() {
        try {
            // activeNotifications aqui chama corretamente o getActiveNotifications() do sistema
            val notifications = activeNotifications.toList().filter {
                it.isClearable && !it.notification.extras.getString("android.title").isNullOrEmpty()
            }
            notificationsLiveData.postValue(notifications)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearNotification(key: String) {
        cancelNotification(key)
        updateNotifications()
    }
}