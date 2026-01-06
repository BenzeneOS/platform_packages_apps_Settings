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

package com.android.settings.fuelgauge

import android.app.settings.SettingsEnums
import android.content.Context
import android.ext.settings.ExtSettings
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.core.SubSettingLauncher

class BatterySpoofGlobalPreferenceController(
    context: Context,
    preferenceKey: String
) : BasePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun updateState(preference: Preference) {
        val level = ExtSettings.BATTERY_SPOOF_LEVEL.get(mContext)
        val spoofCharging = ExtSettings.BATTERY_SPOOF_CHARGING.get(mContext)
        val spoofHealth = ExtSettings.BATTERY_SPOOF_HEALTH.get(mContext)
        val hideMetrics = ExtSettings.BATTERY_SPOOF_HIDE_METRICS.get(mContext)

        val features = mutableListOf<String>()
        if (level >= 0) {
            features.add(mContext.getString(R.string.battery_spoof_global_summary_level, level))
        }
        if (spoofCharging >= 0 || spoofHealth >= 0 || hideMetrics) {
            features.add(mContext.getString(R.string.battery_spoof_global_summary_spoofing))
        }

        preference.summary = if (features.isEmpty()) {
            mContext.getString(R.string.battery_spoof_global_summary_off)
        } else {
            features.joinToString(", ")
        }
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preferenceKey != preference.key) {
            return false
        }

        SubSettingLauncher(preference.context)
            .setDestination(BatterySpoofSettingsFragment::class.java.name)
            .setTitleRes(R.string.battery_spoof_settings_title)
            .setSourceMetricsCategory(SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL)
            .launch()

        return true
    }
}
