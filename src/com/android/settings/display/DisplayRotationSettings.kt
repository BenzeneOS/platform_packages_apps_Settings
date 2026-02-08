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

package com.android.settings.display

import android.app.settings.SettingsEnums
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.widget.CompoundButton
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.android.internal.view.RotationPolicy
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.widget.MainSwitchPreference

@SearchIndexable
class DisplayRotationSettings : SettingsPreferenceFragment(),
    Preference.OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    private lateinit var mainSwitch: MainSwitchPreference
    private lateinit var anglesCategory: PreferenceCategory
    private lateinit var rotation0Pref: SwitchPreferenceCompat
    private lateinit var rotation90Pref: SwitchPreferenceCompat
    private lateinit var rotation180Pref: SwitchPreferenceCompat
    private lateinit var rotation270Pref: SwitchPreferenceCompat

    private var rotationPolicyListener: RotationPolicy.RotationPolicyListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.display_rotation_settings)

        mainSwitch = findPreference(KEY_MAIN_SWITCH)!!
        @Suppress("DEPRECATION")
        mainSwitch.addOnSwitchChangeListener(this)

        anglesCategory = findPreference(KEY_ANGLES_CATEGORY)!!

        val mode = Settings.System.getIntForUser(
            contentResolver,
            Settings.System.ACCELEROMETER_ROTATION_ANGLES,
            ROTATION_0 or ROTATION_90 or ROTATION_270,
            UserHandle.USER_CURRENT
        )

        rotation0Pref = findPreference(KEY_ROTATION_0)!!
        rotation0Pref.isChecked = (mode and ROTATION_0) != 0
        rotation0Pref.onPreferenceChangeListener = this

        rotation90Pref = findPreference(KEY_ROTATION_90)!!
        rotation90Pref.isChecked = (mode and ROTATION_90) != 0
        rotation90Pref.onPreferenceChangeListener = this

        rotation180Pref = findPreference(KEY_ROTATION_180)!!
        rotation180Pref.isChecked = (mode and ROTATION_180) != 0
        rotation180Pref.onPreferenceChangeListener = this

        rotation270Pref = findPreference(KEY_ROTATION_270)!!
        rotation270Pref.isChecked = (mode and ROTATION_270) != 0
        rotation270Pref.onPreferenceChangeListener = this

        updateMainSwitch()
        updateAnglesDependency()
    }

    override fun onResume() {
        super.onResume()
        rotationPolicyListener = object : RotationPolicy.RotationPolicyListener() {
            override fun onChange() {
                updateMainSwitch()
                updateAnglesDependency()
            }
        }
        RotationPolicy.registerRotationPolicyListener(requireContext(), rotationPolicyListener)
    }

    override fun onPause() {
        super.onPause()
        rotationPolicyListener?.let {
            RotationPolicy.unregisterRotationPolicyListener(requireContext(), it)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        RotationPolicy.setRotationLock(
            requireContext(), !isChecked,
            "DisplayRotationSettings#onCheckedChanged"
        )
        updateAnglesDependency()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        var mode = getRotationBitmask()

        val bit = when (preference) {
            rotation0Pref -> ROTATION_0
            rotation90Pref -> ROTATION_90
            rotation180Pref -> ROTATION_180
            rotation270Pref -> ROTATION_270
            else -> return false
        }

        mode = if (newValue as Boolean) mode or bit else mode and bit.inv()

        if (mode == 0) {
            mode = ROTATION_0
            rotation0Pref.isChecked = true
        }

        Settings.System.putIntForUser(
            contentResolver,
            Settings.System.ACCELEROMETER_ROTATION_ANGLES,
            mode,
            UserHandle.USER_CURRENT
        )
        return true
    }

    private fun updateMainSwitch() {
        mainSwitch.isChecked = !RotationPolicy.isRotationLocked(requireContext())
    }

    private fun updateAnglesDependency() {
        val enabled = !RotationPolicy.isRotationLocked(requireContext())
        anglesCategory.isEnabled = enabled
    }

    private fun getRotationBitmask(): Int {
        var mode = 0
        if (rotation0Pref.isChecked) mode = mode or ROTATION_0
        if (rotation90Pref.isChecked) mode = mode or ROTATION_90
        if (rotation180Pref.isChecked) mode = mode or ROTATION_180
        if (rotation270Pref.isChecked) mode = mode or ROTATION_270
        return mode
    }

    override fun getMetricsCategory(): Int = SettingsEnums.DISPLAY

    companion object {
        private const val KEY_MAIN_SWITCH = "auto_rotate_main_switch"
        private const val KEY_ANGLES_CATEGORY = "rotation_angles_category"
        private const val KEY_ROTATION_0 = "rotation_0"
        private const val KEY_ROTATION_90 = "rotation_90"
        private const val KEY_ROTATION_180 = "rotation_180"
        private const val KEY_ROTATION_270 = "rotation_270"

        const val ROTATION_0 = 1
        const val ROTATION_90 = 2
        const val ROTATION_180 = 4
        const val ROTATION_270 = 8

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.display_rotation_settings)
    }
}
