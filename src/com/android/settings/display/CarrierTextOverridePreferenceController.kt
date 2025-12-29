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

package com.android.settings.display

import android.content.Context
import android.provider.Settings
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController

/**
 * Preference controller for setting custom carrier text on the lockscreen.
 */
class CarrierTextOverridePreferenceController(
    context: Context,
    preferenceKey: String
) : BasePreferenceController(context, preferenceKey), Preference.OnPreferenceChangeListener {

    private var preference: EditTextPreference? = null

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference<EditTextPreference>(preferenceKey)?.apply {
            onPreferenceChangeListener = this@CarrierTextOverridePreferenceController
            text = Settings.System.getString(mContext.contentResolver, Settings.System.CARRIER_TEXT_OVERRIDE)
            summary = this@CarrierTextOverridePreferenceController.summary
        }
    }

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        // Disable when carrier text is hidden
        val carrierEnabled = Settings.System.getInt(
            mContext.contentResolver,
            Settings.System.CARRIER_ON_LOCKSCREEN,
            1
        ) != 0
        preference.isEnabled = carrierEnabled
    }

    override fun getSummary(): CharSequence {
        val override = Settings.System.getString(mContext.contentResolver, Settings.System.CARRIER_TEXT_OVERRIDE)
        return if (override.isNullOrEmpty()) {
            mContext.getString(R.string.carrier_text_override_summary_empty)
        } else {
            override
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val text = newValue as String
        Settings.System.putString(mContext.contentResolver, Settings.System.CARRIER_TEXT_OVERRIDE, text)
        this.preference?.summary = if (text.isEmpty()) {
            mContext.getString(R.string.carrier_text_override_summary_empty)
        } else {
            text
        }
        return true
    }
}
