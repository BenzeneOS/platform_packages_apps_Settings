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
import android.os.SystemProperties
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.core.PreferenceControllerMixin
import com.android.settingslib.development.DeveloperOptionsPreferenceController
import com.android.settingslib.development.SystemPropPoker

class SetGpuRendererPreferenceController(
    context: Context
) : DeveloperOptionsPreferenceController(context),
    Preference.OnPreferenceChangeListener,
    PreferenceControllerMixin {

    private val listValues: Array<String> = context.resources.getStringArray(
        com.android.settings.R.array.debug_hw_renderer_values
    )
    private val listSummaries: Array<String> = context.resources.getStringArray(
        com.android.settings.R.array.debug_hw_renderer_entries
    )
    private var preference: ListPreference? = null

    override fun getPreferenceKey(): String = DEBUG_HW_RENDERER_KEY

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        writeDebugHwRendererOptions(newValue)
        updateDebugHwRendererOptions()
        return true
    }

    override fun updateState(preference: Preference) {
        updateDebugHwRendererOptions()
    }

    override fun onDeveloperOptionsSwitchEnabled() {
        preference?.isEnabled = true
    }

    override fun onDeveloperOptionsSwitchDisabled() {
        preference?.isEnabled = false
    }

    private fun writeDebugHwRendererOptions(newValue: Any?) {
        SystemProperties.set(DEBUG_HW_RENDERER_PROPERTY, newValue?.toString() ?: "")
        SystemPropPoker.getInstance().poke()
    }

    private fun updateDebugHwRendererOptions() {
        val value = SystemProperties.get(DEBUG_HW_RENDERER_PROPERTY, "")
        val index = listValues.indexOfFirst { it == value }.takeIf { it >= 0 } ?: 0
        preference?.apply {
            setValue(listValues[index])
            summary = listSummaries[index]
        }
    }

    companion object {
        private const val DEBUG_HW_RENDERER_KEY = "debug_hw_renderer"
        private const val DEBUG_HW_RENDERER_PROPERTY = "debug.hwui.renderer"
    }
}
