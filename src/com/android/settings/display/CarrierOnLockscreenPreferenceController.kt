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
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController
import com.android.settingslib.PrimarySwitchPreference

/**
 * Preference controller for showing/hiding carrier name on the lockscreen.
 * Uses PrimarySwitchPreference to allow navigation to custom text settings.
 */
class CarrierOnLockscreenPreferenceController(
    context: Context,
    preferenceKey: String
) : TogglePreferenceController(context, preferenceKey) {

    private var preference: PrimarySwitchPreference? = null

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun isChecked(): Boolean =
        Settings.System.getInt(mContext.contentResolver, Settings.System.CARRIER_ON_LOCKSCREEN, 1) != 0

    override fun setChecked(isChecked: Boolean): Boolean =
        Settings.System.putInt(mContext.contentResolver, Settings.System.CARRIER_ON_LOCKSCREEN, if (isChecked) 1 else 0)

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        this.preference?.summary = summary
    }

    override fun getSummary(): CharSequence {
        if (!isChecked) {
            return mContext.getString(R.string.switch_off_text)
        }
        val override = Settings.System.getString(mContext.contentResolver, Settings.System.CARRIER_TEXT_OVERRIDE)
        return if (!override.isNullOrEmpty()) override else mContext.getString(R.string.switch_on_text)
    }

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun getSliceHighlightMenuRes(): Int = R.string.menu_key_display
}
