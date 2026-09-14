package com.marinov.watchlauncher.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.lifecycle.MutableLiveData

class WatchNotificationListener : NotificationListenerService() {

    companion object {
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