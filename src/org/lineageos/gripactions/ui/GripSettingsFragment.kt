/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.gripactions.ui

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationManager
import android.content.ComponentName
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.preference.Preference
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import org.lineageos.gripactions.R
import org.lineageos.gripactions.service.GripNotificationService

class GripSettingsFragment : SettingsBasePreferenceFragment() {
    companion object {
        private const val TAG = "GripSettingsFragment"
        private const val DEBUG = true
    }

    private val enable by lazy { findPreference<MainSwitchPreference>("grip_enabled")!! }
    private val testCall by lazy { findPreference<Preference>("test_call")!! }

    private val requestNotifications = registerForActivityResult(RequestPermission()) { granted ->
        if (granted) showTestCall()
    }

    private val notifications by lazy {
        requireContext().getSystemService(NotificationManager::class.java)
    }

    private val listener by lazy {
        ComponentName(requireContext(), GripNotificationService::class.java)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.grip_preferences, rootKey)

        testCall.setOnPreferenceClickListener {
            if (requireContext().checkSelfPermission(POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
                requestNotifications.launch(POST_NOTIFICATIONS)
            } else {
                showTestCall()
            }

            true
        }

        enable.setOnPreferenceChangeListener { _, value ->
            try {
                notifications.setNotificationListenerAccessGranted(listener, value as Boolean)
                if (DEBUG) Log.d(TAG, "Notification access enabled=$value")
                true
            } catch (e: RuntimeException) {
                Log.e(TAG, "Cannot change notification access", e)
                false
            }
        }
    }

    private fun showTestCall() {
        if (!notifications.areNotificationsEnabled()) {
            Toast.makeText(
                requireContext(),
                R.string.test_call_notifications_disabled,
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        TestCallReceiver.show(requireContext())
    }

    override fun onResume() {
        super.onResume()

        enable.isChecked = notifications.isNotificationListenerAccessGranted(listener)
    }
}
