/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.gripactions.ui

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R

class GripSettingsActivity : CollapsingToolbarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return

        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, GripSettingsFragment())
            .commit()
    }
}
