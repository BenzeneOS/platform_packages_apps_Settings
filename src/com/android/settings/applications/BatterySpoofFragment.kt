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

package com.android.settings.applications

import android.app.settings.SettingsEnums
import android.content.pm.GosPackageState
import android.ext.settings.ExtSettings
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.applications.appinfo.RadioButtonAppInfoFragment
import com.android.settingslib.widget.FooterPreference

class BatterySpoofFragment : RadioButtonAppInfoFragment() {

    private var currentLevel = -1

    override fun getMetricsCategory(): Int = SettingsEnums.PAGE_UNKNOWN

    override fun getTitle(): CharSequence = getString(R.string.battery_spoof_title)

    override fun getEntries(): Array<Entry> {
        val ps = GosPackageState.get(mPackageName, mUserId)
        val spoofedLevel = ps?.spoofedBatteryLevel ?: -1
        currentLevel = spoofedLevel

        val globalLevel = ExtSettings.BATTERY_SPOOF_LEVEL.get(requireContext())

        val global = createEntry(ID_GLOBAL, getString(R.string.battery_spoof_option_global)).apply {
            isChecked = spoofedLevel == -1
            summary = if (globalLevel >= 0) {
                getString(R.string.battery_spoof_summary_spoofed, globalLevel)
            } else {
                getString(R.string.battery_spoof_summary_real)
            }
        }

        val real = createEntry(ID_REAL, getString(R.string.battery_spoof_option_real)).apply {
            isChecked = spoofedLevel == -2
        }

        val custom = createEntry(ID_CUSTOM, getString(R.string.battery_spoof_option_custom)).apply {
            isChecked = spoofedLevel >= 0
            if (isChecked) {
                summary = getString(R.string.battery_spoof_summary_spoofed, spoofedLevel)
            }
        }

        return arrayOf(global, real, custom)
    }

    override fun hasFooter(): Boolean = true

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.battery_spoof_footer)
    }

    override fun onEntrySelected(id: Int) {
        when (id) {
            ID_GLOBAL -> setSpoofedLevel(-1)
            ID_REAL -> setSpoofedLevel(-2)
            ID_CUSTOM -> showCustomLevelDialog()
        }
    }

    private fun showCustomLevelDialog() {
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.battery_spoof_dialog, null)
        val seekBar = view.findViewById<SeekBar>(R.id.battery_spoof_seekbar)
        val valueText = view.findViewById<TextView>(R.id.battery_spoof_value)

        val initialValue = if (currentLevel in 0..100) currentLevel else 100
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

        AlertDialog.Builder(context)
            .setTitle(R.string.battery_spoof_option_custom)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                setSpoofedLevel(seekBar.progress)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setSpoofedLevel(level: Int) {
        GosPackageState.edit(mPackageName, mUserId).apply {
            setSpoofedBatteryLevel(level)
            apply()
        }
        if (!refreshUi()) {
            finish()
        }
    }

    companion object {
        private const val ID_GLOBAL = 0
        private const val ID_REAL = 1
        private const val ID_CUSTOM = 2

        fun show(parent: Fragment, packageName: String, userId: Int) {
            val args = Bundle().apply {
                putString("package", packageName)
                putInt("uid", 0)
            }
            val fragment = BatterySpoofFragment().apply {
                arguments = args
            }
            parent.parentFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
}
