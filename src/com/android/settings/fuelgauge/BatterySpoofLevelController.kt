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

import android.app.AlertDialog
import android.content.Context
import android.ext.settings.ExtSettings
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.TextView
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.google.android.material.slider.Slider

class BatterySpoofLevelController(
    context: Context,
    preferenceKey: String
) : BasePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun updateState(preference: Preference) {
        val level = ExtSettings.BATTERY_SPOOF_LEVEL.get(mContext)
        preference.summary = if (level >= 0) {
            mContext.getString(R.string.battery_spoof_level_summary_on, level)
        } else {
            mContext.getString(R.string.battery_spoof_level_summary_off)
        }
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preferenceKey != preference.key) {
            return false
        }
        showLevelDialog(preference)
        return true
    }

    private fun showLevelDialog(preference: Preference) {
        val context = preference.context
        val currentLevel = ExtSettings.BATTERY_SPOOF_LEVEL.get(context)

        val themedContext = ContextThemeWrapper(context, com.google.android.material.R.style.Theme_Material3_DynamicColors_DayNight)
        val view = LayoutInflater.from(themedContext).inflate(R.layout.battery_spoof_dialog, null)
        val slider = view.findViewById<Slider>(R.id.battery_spoof_seekbar)
        val valueText = view.findViewById<TextView>(R.id.battery_spoof_value)

        val initialValue = if (currentLevel >= 1) currentLevel.toFloat() else 100f
        slider.value = initialValue
        valueText.text = "${initialValue.toInt()}%"

        slider.addOnChangeListener { _, value, _ ->
            valueText.text = "${value.toInt()}%"
        }

        AlertDialog.Builder(context, android.R.style.ThemeOverlay_Material_Dialog_Alert)
            .setTitle(R.string.battery_spoof_level_title)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ExtSettings.BATTERY_SPOOF_LEVEL.put(context, slider.value.toInt())
                updateState(preference)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.battery_spoof_disable) { _, _ ->
                ExtSettings.BATTERY_SPOOF_LEVEL.put(context, -1)
                updateState(preference)
            }
            .show()
    }
}
