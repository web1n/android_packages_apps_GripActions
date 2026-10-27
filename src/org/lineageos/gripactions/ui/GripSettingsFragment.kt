/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.gripactions.ui

import android.app.NotificationManager
import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import org.lineageos.gripactions.R
import org.lineageos.gripactions.sensor.GripSensor
import org.lineageos.gripactions.service.GripNotificationService

class GripSettingsFragment : SettingsBasePreferenceFragment() {
    companion object {
        private const val TAG = "GripSettingsFragment"
        private const val DEBUG = true
    }

    private val enable by lazy { findPreference<MainSwitchPreference>("grip_enabled")!! }

    private val notifications by lazy {
        requireContext().getSystemService(NotificationManager::class.java)
    }

    private val listener by lazy {
        ComponentName(requireContext(), GripNotificationService::class.java)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.grip_preferences, rootKey)

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

    override fun onResume() {
        super.onResume()

        enable.isChecked = notifications.isNotificationListenerAccessGranted(listener)
    }

}
