/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.gripactions.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import org.lineageos.gripactions.R

class TestCallReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "TestCallReceiver"
        private const val CHANNEL_ID = "test_calls"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_ANSWER = "org.lineageos.gripactions.TEST_CALL_ANSWER"
        private const val ACTION_DECLINE = "org.lineageos.gripactions.TEST_CALL_DECLINE"

        fun show(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.test_call_channel),
                NotificationManager.IMPORTANCE_HIGH,
            )
            manager.createNotificationChannel(channel)

            if (manager.getNotificationChannel(CHANNEL_ID).importance ==
                NotificationManager.IMPORTANCE_NONE
            ) {
                Toast.makeText(
                    context,
                    R.string.test_call_notifications_disabled,
                    Toast.LENGTH_LONG,
                ).show()
                return
            }

            val caller = Person.Builder()
                .setName(context.getString(R.string.app_name))
                .setImportant(true)
                .build()

            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, GripSettingsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notification = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setCategory(Notification.CATEGORY_CALL)
                .setContentIntent(contentIntent)
                .setFullScreenIntent(contentIntent, false)
                .setStyle(
                    Notification.CallStyle.forIncomingCall(
                        caller,
                        createAction(context, ACTION_DECLINE),
                        createAction(context, ACTION_ANSWER),
                    )
                )
                .setTimeoutAfter(60_000)
                .build()

            try {
                manager.notify(NOTIFICATION_ID, notification)
            } catch (e: IllegalArgumentException) {
                reportFailure(context, e)
            } catch (e: SecurityException) {
                reportFailure(context, e)
            }
        }

        private fun createAction(context: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, TestCallReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun reportFailure(context: Context, error: RuntimeException) {
            Log.e(TAG, "Cannot post test call notification", error)
            Toast.makeText(context, R.string.test_call_failed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val message = when (intent.action) {
            ACTION_ANSWER -> R.string.test_call_answered
            ACTION_DECLINE -> R.string.test_call_declined
            else -> return
        }

        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
