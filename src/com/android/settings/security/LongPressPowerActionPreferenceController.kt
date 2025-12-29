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

package com.android.settings.security

import android.content.Context
import android.provider.Settings
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settings.core.BasePreferenceController

/** Preference controller for long press power action when screen is off. */
class LongPressPowerActionPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key), Preference.OnPreferenceChangeListener {

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun updateState(preference: Preference) {
        val listPreference = preference as ListPreference
        val value = Settings.System.getInt(
            mContext.contentResolver,
            Settings.System.LONG_PRESS_POWER_ACTION,
            0
        )
        listPreference.value = value.toString()
        listPreference.summary = listPreference.entry
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val value = (newValue as String).toInt()
        Settings.System.putInt(
            mContext.contentResolver,
            Settings.System.LONG_PRESS_POWER_ACTION,
            value
        )
        val listPreference = preference as ListPreference
        val index = listPreference.findIndexOfValue(newValue)
        listPreference.summary = listPreference.entries[index]
        return true
    }
}
