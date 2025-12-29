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
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.BasePreferenceController

class BatterySpoofGlobalPreferenceController(
    context: Context,
    preferenceKey: String
) : BasePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun updateState(preference: Preference) {
        val level = ExtSettings.BATTERY_SPOOF_LEVEL.get(mContext)
        preference.summary = if (level >= 0) {
            mContext.getString(R.string.battery_spoof_global_summary_on, level)
        } else {
            mContext.getString(R.string.battery_spoof_global_summary_off)
        }
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preferenceKey != preference.key) {
            return false
        }
        showConfigDialog(preference)
        return true
    }

    private fun showConfigDialog(preference: Preference) {
        val currentLevel = ExtSettings.BATTERY_SPOOF_LEVEL.get(mContext)

        val view = LayoutInflater.from(mContext).inflate(R.layout.battery_spoof_dialog, null)
        val seekBar = view.findViewById<SeekBar>(R.id.battery_spoof_seekbar)
        val valueText = view.findViewById<TextView>(R.id.battery_spoof_value)

        val initialValue = if (currentLevel >= 0) currentLevel else 100
        seekBar.max = 100
        seekBar.progress = initialValue
        valueText.text = "$initialValue%"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                valueText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        AlertDialog.Builder(mContext)
            .setTitle(R.string.battery_spoof_global_title)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ExtSettings.BATTERY_SPOOF_LEVEL.put(mContext, seekBar.progress)
                updateState(preference)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.battery_spoof_option_real) { _, _ ->
                ExtSettings.BATTERY_SPOOF_LEVEL.put(mContext, -1)
                updateState(preference)
            }
            .show()
    }
}
