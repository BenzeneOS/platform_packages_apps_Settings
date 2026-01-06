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

class BatterySpoofJitterController(
    context: Context,
    preferenceKey: String
) : BasePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun updateState(preference: Preference) {
        val value = ExtSettings.BATTERY_SPOOF_JITTER.get(mContext)
        preference.summary = if (value == 0) {
            mContext.getString(R.string.battery_spoof_jitter_summary_off)
        } else {
            mContext.getString(R.string.battery_spoof_jitter_summary, value)
        }
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preferenceKey != preference.key) {
            return false
        }
        showJitterDialog(preference)
        return true
    }

    private fun showJitterDialog(preference: Preference) {
        val context = preference.context
        val currentValue = ExtSettings.BATTERY_SPOOF_JITTER.get(context)

        val themedContext = ContextThemeWrapper(context, com.google.android.material.R.style.Theme_Material3_DynamicColors_DayNight)
        val view = LayoutInflater.from(themedContext).inflate(R.layout.battery_spoof_jitter_dialog, null)
        val slider = view.findViewById<Slider>(R.id.jitter_seekbar)
        val valueText = view.findViewById<TextView>(R.id.jitter_value)

        slider.value = currentValue.toFloat()
        updateValueText(valueText, currentValue)

        slider.addOnChangeListener { _, value, _ ->
            updateValueText(valueText, value.toInt())
        }

        AlertDialog.Builder(context, android.R.style.ThemeOverlay_Material_Dialog_Alert)
            .setTitle(R.string.battery_spoof_jitter_title)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ExtSettings.BATTERY_SPOOF_JITTER.put(context, slider.value.toInt())
                updateState(preference)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateValueText(textView: TextView, value: Int) {
        textView.text = if (value == 0) {
            mContext.getString(R.string.battery_spoof_jitter_value_off)
        } else {
            "±${value}%"
        }
    }
}
