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
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class BatterySpoofSettingsFragment : DashboardFragment() {

    override fun getPreferenceScreenResId(): Int = R.xml.battery_spoof_settings

    override fun getLogTag(): String = TAG

    override fun getMetricsCategory(): Int = SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL

    override fun createPreferenceControllers(context: Context): List<AbstractPreferenceController> {
        return listOf(
            BatterySpoofLevelController(context, KEY_LEVEL),
            BatterySpoofJitterController(context, KEY_JITTER),
            BatterySpoofJitterIntervalController(context, KEY_JITTER_INTERVAL),
            BatterySpoofChargingController(context, KEY_CHARGING),
            BatterySpoofHealthController(context, KEY_HEALTH),
            BatterySpoofHideMetricsController(context, KEY_HIDE_METRICS)
        )
    }

    companion object {
        private const val TAG = "BatterySpoofSettings"

        private const val KEY_LEVEL = "battery_spoof_level"
        private const val KEY_JITTER = "battery_spoof_jitter"
        private const val KEY_JITTER_INTERVAL = "battery_spoof_jitter_interval"
        private const val KEY_CHARGING = "battery_spoof_charging"
        private const val KEY_HEALTH = "battery_spoof_health"
        private const val KEY_HIDE_METRICS = "battery_spoof_hide_metrics"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.battery_spoof_settings)
    }
}
