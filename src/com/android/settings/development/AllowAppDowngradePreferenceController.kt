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

package com.android.settings.development

import android.content.Context
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.android.settings.core.PreferenceControllerMixin
import com.android.settingslib.development.DeveloperOptionsPreferenceController

class AllowAppDowngradePreferenceController(context: Context) :
    DeveloperOptionsPreferenceController(context),
    Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    override fun getPreferenceKey(): String = KEY

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        Settings.Global.putInt(
            mContext.contentResolver,
            Settings.Global.PM_DOWNGRADE_ALLOWED,
            if (newValue as Boolean) 1 else 0
        )
        return true
    }

    override fun updateState(preference: Preference) {
        val enabled = Settings.Global.getInt(
            mContext.contentResolver,
            Settings.Global.PM_DOWNGRADE_ALLOWED, 0
        )
        (mPreference as TwoStatePreference).isChecked = enabled != 0
    }

    override fun onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled()
        Settings.Global.putInt(
            mContext.contentResolver,
            Settings.Global.PM_DOWNGRADE_ALLOWED, 0
        )
        (mPreference as TwoStatePreference).isChecked = false
    }

    companion object {
        private const val KEY = "allow_app_downgrade"
    }
}
