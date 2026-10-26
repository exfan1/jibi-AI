package com.jibi.ai.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class JibiNotificationListener : NotificationListenerService() {
    companion object {
        private val items = ArrayDeque<String>()
        fun snapshot(): List<String> = synchronized(items) { items.toList() }
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val text = sbn.notification.extras?.getCharSequence("android.text")?.toString().orEmpty()
        if (text.isNotBlank()) synchronized(items) { items.addFirst(text.take(300)); while (items.size > 30) items.removeLast() }
    }
}
