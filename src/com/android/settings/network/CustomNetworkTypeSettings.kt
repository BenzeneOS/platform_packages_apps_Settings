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

package com.android.settings.network

import android.app.settings.SettingsEnums
import android.os.Bundle
import android.provider.Settings
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class CustomNetworkTypeSettings : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private lateinit var customTextPref: EditTextPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.custom_network_type_settings)

        val resolver = contentResolver

        customTextPref = findPreference(KEY_CUSTOM_TEXT)!!
        val currentText = Settings.System.getString(
            resolver, Settings.System.CUSTOM_NETWORK_TYPE_TEXT
        ) ?: ""
        customTextPref.text = currentText
        updateSummary(currentText)
        customTextPref.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val resolver = contentResolver

        return when (preference) {
            customTextPref -> {
                val text = (newValue as String).trim()
                Settings.System.putString(
                    resolver, Settings.System.CUSTOM_NETWORK_TYPE_TEXT, text
                )
                updateSummary(text)
                true
            }
            else -> false
        }
    }

    private fun updateSummary(currentText: String) {
        customTextPref.summary = if (currentText.isEmpty()) {
            getString(R.string.custom_network_type_text_summary)
        } else {
            getString(R.string.custom_network_type_text_summary_with_value, currentText)
        }
    }

    override fun getMetricsCategory(): Int = SettingsEnums.MOBILE_NETWORK

    companion object {
        private const val KEY_CUSTOM_TEXT = "custom_network_type_text"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.custom_network_type_settings)
    }
}
