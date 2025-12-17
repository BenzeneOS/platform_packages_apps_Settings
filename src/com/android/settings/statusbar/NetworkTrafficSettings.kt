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

package com.android.settings.statusbar

import android.app.settings.SettingsEnums
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import app.benzeneos.providers.BenzeneSettings
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class NetworkTrafficSettings : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private lateinit var enabledPref: SwitchPreferenceCompat
    private lateinit var modePref: ListPreference
    private lateinit var autohidePref: SwitchPreferenceCompat
    private lateinit var unitsPref: ListPreference
    private lateinit var refreshIntervalPref: ListPreference
    private lateinit var hideArrowPref: SwitchPreferenceCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.network_traffic_settings)

        val resolver = contentResolver

        enabledPref = findPreference(KEY_ENABLED)!!
        enabledPref.isChecked = BenzeneSettings.System.getInt(
            resolver, BenzeneSettings.System.NETWORK_TRAFFIC_ENABLED, 0
        ) == 1
        enabledPref.onPreferenceChangeListener = this

        modePref = findPreference(KEY_MODE)!!
        val mode = BenzeneSettings.System.getInt(
            resolver, BenzeneSettings.System.NETWORK_TRAFFIC_MODE, 0
        )
        modePref.value = mode.toString()
        modePref.summary = modePref.entry
        modePref.onPreferenceChangeListener = this

        autohidePref = findPreference(KEY_AUTOHIDE)!!
        autohidePref.isChecked = BenzeneSettings.System.getInt(
            resolver, BenzeneSettings.System.NETWORK_TRAFFIC_AUTOHIDE, 0
        ) == 1
        autohidePref.onPreferenceChangeListener = this

        unitsPref = findPreference(KEY_UNITS)!!
        val units = BenzeneSettings.System.getInt(
            resolver, BenzeneSettings.System.NETWORK_TRAFFIC_UNITS, 1
        )
        unitsPref.value = units.toString()
        unitsPref.summary = unitsPref.entry
        unitsPref.onPreferenceChangeListener = this

        refreshIntervalPref = findPreference(KEY_REFRESH_INTERVAL)!!
        val interval = BenzeneSettings.System.getInt(
            resolver, BenzeneSettings.System.NETWORK_TRAFFIC_REFRESH_INTERVAL, 2
        )
        refreshIntervalPref.value = interval.toString()
        refreshIntervalPref.summary = refreshIntervalPref.entry
        refreshIntervalPref.onPreferenceChangeListener = this

        hideArrowPref = findPreference(KEY_HIDE_ARROW)!!
        hideArrowPref.isChecked = BenzeneSettings.System.getInt(
            resolver, BenzeneSettings.System.NETWORK_TRAFFIC_HIDEARROW, 0
        ) == 1
        hideArrowPref.onPreferenceChangeListener = this

        updateDependencies(enabledPref.isChecked)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val resolver = contentResolver

        return when (preference) {
            enabledPref -> {
                val enabled = newValue as Boolean
                BenzeneSettings.System.putInt(
                    resolver, BenzeneSettings.System.NETWORK_TRAFFIC_ENABLED, if (enabled) 1 else 0
                )
                updateDependencies(enabled)
                true
            }
            modePref -> {
                val mode = (newValue as String).toInt()
                BenzeneSettings.System.putInt(
                    resolver, BenzeneSettings.System.NETWORK_TRAFFIC_MODE, mode
                )
                val index = modePref.findIndexOfValue(newValue)
                modePref.summary = modePref.entries[index]
                true
            }
            autohidePref -> {
                val autohide = newValue as Boolean
                BenzeneSettings.System.putInt(
                    resolver, BenzeneSettings.System.NETWORK_TRAFFIC_AUTOHIDE, if (autohide) 1 else 0
                )
                true
            }
            unitsPref -> {
                val units = (newValue as String).toInt()
                BenzeneSettings.System.putInt(
                    resolver, BenzeneSettings.System.NETWORK_TRAFFIC_UNITS, units
                )
                val index = unitsPref.findIndexOfValue(newValue)
                unitsPref.summary = unitsPref.entries[index]
                true
            }
            refreshIntervalPref -> {
                val interval = (newValue as String).toInt()
                BenzeneSettings.System.putInt(
                    resolver, BenzeneSettings.System.NETWORK_TRAFFIC_REFRESH_INTERVAL, interval
                )
                val index = refreshIntervalPref.findIndexOfValue(newValue)
                refreshIntervalPref.summary = refreshIntervalPref.entries[index]
                true
            }
            hideArrowPref -> {
                val hideArrow = newValue as Boolean
                BenzeneSettings.System.putInt(
                    resolver, BenzeneSettings.System.NETWORK_TRAFFIC_HIDEARROW, if (hideArrow) 1 else 0
                )
                true
            }
            else -> false
        }
    }

    private fun updateDependencies(enabled: Boolean) {
        modePref.isEnabled = enabled
        autohidePref.isEnabled = enabled
        unitsPref.isEnabled = enabled
        refreshIntervalPref.isEnabled = enabled
        hideArrowPref.isEnabled = enabled
    }

    override fun getMetricsCategory(): Int = SettingsEnums.DISPLAY

    companion object {
        private const val KEY_ENABLED = "network_traffic_enabled"
        private const val KEY_MODE = "network_traffic_mode"
        private const val KEY_AUTOHIDE = "network_traffic_autohide"
        private const val KEY_UNITS = "network_traffic_units"
        private const val KEY_REFRESH_INTERVAL = "network_traffic_refresh_interval"
        private const val KEY_HIDE_ARROW = "network_traffic_hidearrow"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.network_traffic_settings)
    }
}
