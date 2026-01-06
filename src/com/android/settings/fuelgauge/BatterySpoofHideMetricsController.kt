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

import android.content.Context
import android.ext.settings.ExtSettings
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.android.settings.core.TogglePreferenceController

class BatterySpoofHideMetricsController(
    context: Context,
    preferenceKey: String
) : TogglePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun isChecked(): Boolean {
        return ExtSettings.BATTERY_SPOOF_HIDE_METRICS.get(mContext)
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        ExtSettings.BATTERY_SPOOF_HIDE_METRICS.put(mContext, isChecked)
        return true
    }

    override fun getSliceHighlightMenuRes(): Int = 0

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        if (preference is TwoStatePreference) {
            preference.isChecked = isChecked
        }
    }
}
