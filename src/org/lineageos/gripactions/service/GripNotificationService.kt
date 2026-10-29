/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.gripactions.service

import android.app.Notification
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.Display
import org.lineageos.gripactions.ScreenStateMonitor
import org.lineageos.gripactions.sensor.GripSensor
import org.lineageos.gripactions.sensor.GripSensor.Side
import org.lineageos.gripactions.sensor.GripSensor.State
import org.lineageos.gripactions.ui.CallActionsOverlay

class GripNotificationService : NotificationListenerService() {
    companion object {
        private const val TAG = "GripNotificationService"
        private const val DEBUG = true
    }

    private val grip by lazy { GripSensor(this) }
    private val screen by lazy { ScreenStateMonitor(this) }
    private val overlay by lazy { CallActionsOverlay(this) }
    private val calls = linkedMapOf<String, StatusBarNotification>()
    private val defaultDisplay by lazy {
        getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
    }
    private var gripState = State.UNKNOWN
    private var side = Side.RIGHT
    private var connected = false
    private var listening = false

    private val screenListener = ScreenStateMonitor.Listener { updateGripListening() }

    private val gripListener = GripSensor.Listener { state ->
        if (!listening || !state.isSingleHanded()) return@Listener

        gripState = state
        side = resolveGripSide()

        updateOverlay()
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
        if (DEBUG && notification.category == Notification.CATEGORY_CALL) logCall(sbn)
        if (isIncomingCall(notification)) {
            calls[sbn.key] = sbn
        } else if (calls.remove(sbn.key) == null) {
            return
        }

        updateOverlay()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (calls.remove(sbn.key) != null) {
            updateOverlay()
        }

        if (DEBUG && sbn.notification.category == Notification.CATEGORY_CALL) {
            Log.d(TAG, "Call notification removed: key=${sbn.key}")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        side = resolveGripSide()
        overlay.dismiss()
        updateOverlay()
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }

    private fun resolveGripSide(): Side {
        val rotation = defaultDisplay.rotation
        val result = gripState.resolveSide(rotation) ?: Side.RIGHT
        if (DEBUG) {
            Log.d(TAG, "Grip mapping: state=$gripState rotation=$rotation side=$result")
        }
        return result
    }

    private fun isIncomingCall(notification: Notification): Boolean {
        if (notification.category != Notification.CATEGORY_CALL ||
            notification.actions.isNullOrEmpty()) return false

        return when (notification.extras.getInt(Notification.EXTRA_CALL_TYPE, 0)) {
            Notification.CallStyle.CALL_TYPE_INCOMING -> true
            0 -> notification.fullScreenIntent != null
            else -> false
        }
    }

    private fun logCall(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val callType = notification.extras.getInt(Notification.EXTRA_CALL_TYPE, 0)
        Log.d(TAG, "Call notification: key=${sbn.key} type=$callType " +
            "actions=${notification.actions?.size ?: 0}")
        notification.actions?.forEachIndexed { index, action ->
            Log.d(TAG, "Action[$index]: title=${action.title} icon=${action.getIcon()} " +
                "semanticAction=${action.semanticAction} pendingIntent=${action.actionIntent}")
        }
    }

    private fun updateOverlay() {
        val notification = calls.values.lastOrNull()

        if (listening && notification != null) {
            overlay.show(notification, side)
        } else {
            overlay.dismiss()
        }
    }

    private fun updateGripListening() {
        val shouldListen = connected && screen.isReady()
        if (listening == shouldListen) return
        listening = shouldListen

        if (listening) {
            grip.addListener(gripListener)
        } else {
            grip.removeListener(gripListener)
            gripState = State.UNKNOWN
            side = Side.RIGHT
            overlay.dismiss()
        }
    }

    private fun stopListening() {
        if (connected) {
            connected = false
            screen.removeListener(screenListener)
        }

        updateGripListening()
        calls.clear()
    }
}
