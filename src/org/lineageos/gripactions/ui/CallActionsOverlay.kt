/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.gripactions.ui

import android.app.Notification
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View.MeasureSpec
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.LinearLayout
import android.widget.FrameLayout
import org.lineageos.gripactions.R
import org.lineageos.gripactions.sensor.GripSensor.Side

internal class CallActionsOverlay(context: Context) {
    companion object {
        private const val TAG = "CallActionsOverlay"
        private const val DEBUG = true
    }

    private val windowContext = context.createWindowContext(
        context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY),
        LayoutParams.TYPE_SYSTEM_DIALOG,
        null,
    ).apply { setTheme(context.applicationInfo.theme) }
    private val windows = windowContext.getSystemService(WindowManager::class.java)
    private val inflater = LayoutInflater.from(windowContext)
    private var view: FrameLayout? = null
    private var shownNotification: StatusBarNotification? = null
    private val dragListener by lazy { OverlayDragListener(windowContext, windows) { view } }

    fun show(notification: StatusBarNotification, side: Side) {
        val actions = notification.notification.actions.orEmpty()
        if (actions.isEmpty()) {
            dismiss()
            return
        }

        try {
            if (view != null && shownNotification === notification) {
                moveTo(side)
                return
            }

            val root = view ?: inflater.inflate(R.layout.call_actions_overlay, null) as FrameLayout
            bindActions(root, notification, actions)
            val params = createLayoutParams(root, side)

            if (view == null) {
                root.setOnTouchListener(dragListener)
                windows.addView(root, params)
                view = root
            } else {
                windows.updateViewLayout(root, params)
            }
            shownNotification = notification

            if (DEBUG) Log.d(TAG, "Showing ${actions.size} actions on $side")
        } catch (e: Exception) {
            Log.e(TAG, "Cannot show call actions", e)
            dismiss()
        }
    }

    fun dismiss() {
        shownNotification = null
        val root = view ?: return

        try {
            windows.removeViewImmediate(root)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Call actions window was already removed", e)
        }

        view = null
    }

    private fun moveTo(side: Side) {
        val root = view ?: return
        val params = root.layoutParams as LayoutParams
        val gravity = side.gravity or Gravity.CENTER_VERTICAL
        if (params.gravity == gravity) return

        params.gravity = gravity
        windows.updateViewLayout(root, params)
    }

    private fun bindActions(
        root: FrameLayout,
        notification: StatusBarNotification,
        actions: Array<out Notification.Action>,
    ) {
        val container = root.findViewById<LinearLayout>(R.id.call_actions)
        val source = windowContext.createPackageContext(notification.packageName, 0)
        container.removeAllViews()

        actions.forEachIndexed { index, action ->
            val button = (inflater.inflate(R.layout.call_action, container, false) as CallActionButton)
                .apply {
                    bind(source, action)
                    setOnTouchListener(dragListener)
                }
            if (index > 0) {
                (button.layoutParams as LinearLayout.LayoutParams).topMargin = dp(8)
            }

            container.addView(button)
        }
    }

    private fun createLayoutParams(root: FrameLayout, side: Side): LayoutParams {
        val width = dp(96)
        val maxHeight = windows.currentWindowMetrics.bounds.height() / 2
        root.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )

        return LayoutParams(
            width,
            root.measuredHeight.coerceAtMost(maxHeight),
            LayoutParams.TYPE_SYSTEM_DIALOG,
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = side.gravity or Gravity.CENTER_VERTICAL
            x = 0
            y = dragListener.offsetFor(height)
            setTitle("GripActions call actions")
        }
    }

    private fun dp(value: Int): Int =
        (value * windowContext.resources.displayMetrics.density + 0.5f).toInt()

    private val Side.gravity: Int
        get() = when (this) {
            Side.LEFT -> Gravity.LEFT
            Side.RIGHT -> Gravity.RIGHT
        }
}
