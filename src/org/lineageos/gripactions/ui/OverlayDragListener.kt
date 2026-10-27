/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.gripactions.ui

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import kotlin.math.abs
import kotlin.math.roundToInt

internal class OverlayDragListener(
    context: Context,
    private val windows: WindowManager,
    private val getRoot: () -> View?,
) : View.OnTouchListener {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var verticalOffset = 0
    private var startX = 0f
    private var startY = 0f
    private var startOffset = 0
    private var dragging = false

    override fun onTouch(target: View, event: MotionEvent): Boolean {
        val root = getRoot() ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                target.parent?.requestDisallowInterceptTouchEvent(true)
                target.isPressed = true
                startX = event.rawX
                startY = event.rawY
                startOffset = verticalOffset
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val distance = event.rawY - startY
                if (abs(distance) > touchSlop || abs(event.rawX - startX) > touchSlop) {
                    dragging = true
                }
                if (dragging) target.isPressed = false
                if (dragging && root.isAttachedToWindow) {
                    val params = root.layoutParams as LayoutParams
                    verticalOffset = clampOffset(startOffset + distance.roundToInt(), params.height)
                    params.y = verticalOffset
                    windows.updateViewLayout(root, params)
                }
            }
            MotionEvent.ACTION_UP -> {
                target.isPressed = false
                if (!dragging) target.performClick()
                dragging = false
            }
            MotionEvent.ACTION_CANCEL -> {
                target.isPressed = false
                dragging = false
            }
        }
        return true
    }

    fun offsetFor(height: Int): Int {
        verticalOffset = clampOffset(verticalOffset, height)
        return verticalOffset
    }

    private fun clampOffset(offset: Int, height: Int): Int {
        val metrics = windows.currentWindowMetrics
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
        )
        val availableHeight = metrics.bounds.height() - insets.top - insets.bottom
        val limit = ((availableHeight - height) / 2).coerceAtLeast(0)
        return offset.coerceIn(-limit, limit)
    }
}
