/*
 * Copyright (C) 2025 Amaan Qureshi <contact@amaanq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.statusbar

import android.content.Context
import app.benzeneos.providers.BenzeneSettings
import com.android.settings.core.TogglePreferenceController

/**
 * Master preference controller for double tap to sleep.
 * This controls both status bar and lockscreen double tap to sleep at once.
 */
class DoubleTapSleepPreferenceController(context: Context, key: String) :
    TogglePreferenceController(context, key) {

    override fun isChecked(): Boolean {
        // Master is on if either is enabled
        val statusBar = BenzeneSettings.System.getInt(
            mContext.contentResolver,
            BenzeneSettings.System.DOUBLE_TAP_SLEEP_STATUS_BAR,
            0
        )
        val lockscreen = BenzeneSettings.System.getInt(
            mContext.contentResolver,
            BenzeneSettings.System.DOUBLE_TAP_SLEEP_LOCKSCREEN,
            0
        )
        return statusBar == 1 || lockscreen == 1
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        // Master toggle enables/disables both at once
        val value = if (isChecked) 1 else 0
        val statusBarResult = BenzeneSettings.System.putInt(
            mContext.contentResolver,
            BenzeneSettings.System.DOUBLE_TAP_SLEEP_STATUS_BAR,
            value
        )
        val lockscreenResult = BenzeneSettings.System.putInt(
            mContext.contentResolver,
            BenzeneSettings.System.DOUBLE_TAP_SLEEP_LOCKSCREEN,
            value
        )
        return statusBarResult && lockscreenResult
    }

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun getSliceHighlightMenuRes(): Int = 0
}
