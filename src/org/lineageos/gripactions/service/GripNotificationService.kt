/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.gripactions.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.lineageos.gripactions.ScreenStateMonitor
import org.lineageos.gripactions.sensor.GripSensor

class GripNotificationService : NotificationListenerService() {
    companion object {
        private const val TAG = "GripNotificationService"
        private const val DEBUG = true
    }

    private val grip by lazy { GripSensor(this) }
    private val screen by lazy { ScreenStateMonitor(this) }
    private var connected = false
    private var listening = false

    private val screenListener = ScreenStateMonitor.Listener { updateGripListening() }

    private val gripListener = GripSensor.Listener { state ->
        if (!listening) return@Listener

        if (DEBUG) Log.d(TAG, "Grip changed: $state")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        if (DEBUG) Log.d(TAG, "Notification listener connected")
        if (!connected) {
            connected = true
            screen.addListener(screenListener)
        }

        updateGripListening()
        activeNotifications?.forEach { onNotificationPosted(it) }
    }

    override fun onListenerDisconnected() {
        stopListening()

        if (DEBUG) Log.d(TAG, "Notification listener disconnected")

        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        if (notification.category != Notification.CATEGORY_CALL) return

        val callType = notification.extras.getInt(Notification.EXTRA_CALL_TYPE, 0)
        if (DEBUG) {
            Log.d(TAG, "Call notification: key=${sbn.key} type=$callType " +
                "actions=${notification.actions?.size ?: 0}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (DEBUG && sbn.notification.category == Notification.CATEGORY_CALL) {
            Log.d(TAG, "Call notification removed: key=${sbn.key}")
        }
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }

    private fun updateGripListening() {
        val shouldListen = connected && screen.isReady()
        if (listening == shouldListen) return
        listening = shouldListen

        if (listening) {
            grip.addListener(gripListener)
        } else {
            grip.removeListener(gripListener)
        }
    }

    private fun stopListening() {
        if (connected) {
            connected = false
            screen.removeListener(screenListener)
        }

        updateGripListening()
    }

}
