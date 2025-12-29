/*
 * Copyright (C) 2025 Amaan Qureshi <contact@amaanq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.datetime

import android.content.Context
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController
import com.android.settingslib.PrimarySwitchPreference

class ClockSecondsEntryPreferenceController(context: Context, key: String) :
    TogglePreferenceController(context, key) {

    private var preference: PrimarySwitchPreference? = null

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun isChecked(): Boolean {
        val statusBar = Settings.Secure.getInt(
            mContext.contentResolver, CLOCK_SECONDS, 0
        ) != 0
        val qs = Settings.Secure.getInt(
            mContext.contentResolver, QS_HEADER_CLOCK_SECONDS, 0
        ) != 0
        return statusBar || qs
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        val value = if (isChecked) 1 else 0
        val statusBarResult = Settings.Secure.putInt(
            mContext.contentResolver, CLOCK_SECONDS, value
        )
        val qsResult = Settings.Secure.putInt(
            mContext.contentResolver, QS_HEADER_CLOCK_SECONDS, value
        )
        return statusBarResult && qsResult
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)

        val statusBar = Settings.Secure.getInt(
            mContext.contentResolver, CLOCK_SECONDS, 0
        ) != 0
        val qs = Settings.Secure.getInt(
            mContext.contentResolver, QS_HEADER_CLOCK_SECONDS, 0
        ) != 0

        val summary = when {
            statusBar && qs -> mContext.getString(R.string.clock_seconds_both_enabled)
            statusBar -> mContext.getString(R.string.clock_seconds_status_bar_title)
            qs -> mContext.getString(R.string.clock_seconds_qs_title)
            else -> mContext.getString(R.string.switch_off_text)
        }
        preference.summary = summary
    }

    override fun getSliceHighlightMenuRes(): Int = R.string.menu_key_system

    companion object {
        private const val CLOCK_SECONDS = "clock_seconds"
        private const val QS_HEADER_CLOCK_SECONDS = "qs_header_clock_seconds"
    }
}
