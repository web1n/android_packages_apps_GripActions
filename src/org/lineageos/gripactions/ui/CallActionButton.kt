/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.gripactions.ui

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageButton
import com.android.internal.util.ContrastColorUtil

internal class CallActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ImageButton(context, attrs) {
    companion object {
        private const val TAG = "CallActionButton"
        private const val DEBUG = true
    }

    fun bind(source: Context, action: Notification.Action) {
        val icon = loadIcon(source, action)
        if (icon != null) {
            setImageDrawable(icon)
        } else {
            setImageResource(com.android.internal.R.drawable.ic_action_open)
        }

        contentDescription = action.title
        tooltipText = action.title
        applyActionColors(action)
        setOnClickListener { sendAction(action) }
    }

    private fun loadIcon(source: Context, action: Notification.Action): Drawable? {
        return try {
            action.getIcon()?.loadDrawable(source)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Cannot load call action icon", e)
            null
        }
    }

    private fun applyActionColors(action: Notification.Action) {
        val color = Notification.Builder.getFullLengthSpanColor(action.title, null) ?: return
        val ripple = background.mutate() as? RippleDrawable ?: return
        val circle = ripple.getDrawable(0) as? GradientDrawable ?: return
        circle.setColor(color)

        val iconColor = ContrastColorUtil.resolvePrimaryColor(
            context, color, resources.configuration.isNightModeActive
        )
        imageTintList = ColorStateList.valueOf(iconColor)
    }

    private fun sendAction(action: Notification.Action) {
        try {
            val options = ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
            )
            action.actionIntent.send(context, 0, null, null, null, null, options.toBundle())
            if (DEBUG) Log.d(TAG, "Sent call action: ${action.title}")
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "Call action expired", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot execute call action", e)
        }
    }
}
