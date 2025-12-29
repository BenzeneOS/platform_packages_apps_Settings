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

package com.android.settings.fuelgauge.batterysaver

import android.app.settings.SettingsEnums
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.provider.Settings
import android.view.Display
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import kotlin.math.roundToInt

@SearchIndexable
class BatterySaverOptionsFragment : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    // Display
    private lateinit var allowAnimationPref: SwitchPreferenceCompat
    private var refreshRatePref: ListPreference? = null
    private lateinit var allowBlurPref: SwitchPreferenceCompat
    private lateinit var allowHbmPref: SwitchPreferenceCompat
    private lateinit var allowAodPref: SwitchPreferenceCompat
    private lateinit var allowNightModeOffPref: SwitchPreferenceCompat
    private lateinit var allowBrightnessPref: SwitchPreferenceCompat

    // Performance
    private lateinit var allowLaunchBoostPref: SwitchPreferenceCompat
    private lateinit var allowStandbyPref: SwitchPreferenceCompat
    private lateinit var allowBackgroundPref: SwitchPreferenceCompat
    private lateinit var disableQuickDozePref: SwitchPreferenceCompat

    // Network
    private lateinit var allowNetworkPref: SwitchPreferenceCompat
    private lateinit var allowDataPref: SwitchPreferenceCompat

    // Sensors
    private lateinit var allowVibrationPref: SwitchPreferenceCompat
    private lateinit var allowSoundtriggerPref: SwitchPreferenceCompat
    private lateinit var allowSensorsPref: SwitchPreferenceCompat
    private lateinit var allowAttentionPref: SwitchPreferenceCompat
    private lateinit var allowCameraRotatePref: SwitchPreferenceCompat

    // Background tasks
    private lateinit var allowFullBackupPref: SwitchPreferenceCompat
    private lateinit var allowKvBackupPref: SwitchPreferenceCompat

    // Location
    private lateinit var allowLocationPref: SwitchPreferenceCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.battery_saver_options)
        setHasOptionsMenu(true)
        initPreferences()
        loadSettings()
    }

    private fun initPreferences() {
        // Display
        allowAnimationPref = findPreference(KEY_ALLOW_ANIMATION)!!
        allowAnimationPref.onPreferenceChangeListener = this

        // Setup refresh rate preference based on device capabilities
        setupRefreshRatePreference()

        allowBlurPref = findPreference(KEY_ALLOW_BLUR)!!
        allowBlurPref.onPreferenceChangeListener = this

        allowHbmPref = findPreference(KEY_ALLOW_HBM)!!
        allowHbmPref.onPreferenceChangeListener = this

        allowAodPref = findPreference(KEY_ALLOW_AOD)!!
        allowAodPref.onPreferenceChangeListener = this

        allowNightModeOffPref = findPreference(KEY_ALLOW_NIGHT_MODE_OFF)!!
        allowNightModeOffPref.onPreferenceChangeListener = this

        allowBrightnessPref = findPreference(KEY_ALLOW_BRIGHTNESS)!!
        allowBrightnessPref.onPreferenceChangeListener = this

        // Performance
        allowLaunchBoostPref = findPreference(KEY_ALLOW_LAUNCH_BOOST)!!
        allowLaunchBoostPref.onPreferenceChangeListener = this

        allowStandbyPref = findPreference(KEY_ALLOW_STANDBY)!!
        allowStandbyPref.onPreferenceChangeListener = this

        allowBackgroundPref = findPreference(KEY_ALLOW_BACKGROUND)!!
        allowBackgroundPref.onPreferenceChangeListener = this

        disableQuickDozePref = findPreference(KEY_DISABLE_QUICK_DOZE)!!
        disableQuickDozePref.onPreferenceChangeListener = this

        // Network
        allowNetworkPref = findPreference(KEY_ALLOW_NETWORK)!!
        allowNetworkPref.onPreferenceChangeListener = this

        allowDataPref = findPreference(KEY_ALLOW_DATA)!!
        allowDataPref.onPreferenceChangeListener = this

        // Sensors
        allowVibrationPref = findPreference(KEY_ALLOW_VIBRATION)!!
        allowVibrationPref.onPreferenceChangeListener = this

        allowSoundtriggerPref = findPreference(KEY_ALLOW_SOUNDTRIGGER)!!
        allowSoundtriggerPref.onPreferenceChangeListener = this

        allowSensorsPref = findPreference(KEY_ALLOW_SENSORS)!!
        allowSensorsPref.onPreferenceChangeListener = this

        allowAttentionPref = findPreference(KEY_ALLOW_ATTENTION)!!
        allowAttentionPref.onPreferenceChangeListener = this

        allowCameraRotatePref = findPreference(KEY_ALLOW_CAMERA_ROTATE)!!
        allowCameraRotatePref.onPreferenceChangeListener = this

        // Background tasks
        allowFullBackupPref = findPreference(KEY_ALLOW_FULL_BACKUP)!!
        allowFullBackupPref.onPreferenceChangeListener = this

        allowKvBackupPref = findPreference(KEY_ALLOW_KV_BACKUP)!!
        allowKvBackupPref.onPreferenceChangeListener = this

        // Location
        allowLocationPref = findPreference(KEY_ALLOW_LOCATION)!!
        allowLocationPref.onPreferenceChangeListener = this
    }

    private fun setupRefreshRatePreference() {
        val pref: ListPreference? = findPreference(KEY_REFRESH_RATE)
        if (pref == null) return

        // Get supported refresh rates from display
        val displayManager = requireContext().getSystemService(DisplayManager::class.java)
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        val modes = display?.supportedModes ?: emptyArray()

        // Get device's max refresh rate
        val maxRefreshRate = modes
            .map { it.refreshRate.roundToInt() }
            .maxOrNull() ?: 60

        // If device only supports 60Hz (or less), hide the preference entirely
        if (maxRefreshRate <= 60) {
            pref.isVisible = false
            refreshRatePref = null
            return
        }

        // Build dynamic entries with standard refresh rate tiers
        // Include 60, 90, 120 as standard tiers (if below device max), plus device max
        val standardTiers = listOf(60, 90, 120)
        val availableRates = (standardTiers.filter { it < maxRefreshRate } + maxRefreshRate)
            .distinct()
            .sorted()

        val entries = mutableListOf<String>()
        val values = mutableListOf<String>()

        // Add all available rates as options (sorted low to high)
        for (rate in availableRates) {
            entries.add("$rate Hz")
            values.add(rate.toString())
        }

        // Add "No limit" option (value = 0 means no cap)
        entries.add(getString(R.string.battery_saver_refresh_rate_no_limit))
        values.add("0")

        pref.entries = entries.toTypedArray()
        pref.entryValues = values.toTypedArray()
        pref.onPreferenceChangeListener = this

        refreshRatePref = pref
    }

    private fun loadSettings() {
        val resolver = contentResolver

        // Display
        allowAnimationPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_ANIMATION, 0
        ) == 1

        refreshRatePref?.let { pref ->
            // Default is 60 Hz (most restrictive), 0 means no limit
            val refreshRate = Settings.Global.getInt(
                resolver, SETTING_REFRESH_RATE_CAP, 60
            )
            pref.value = refreshRate.toString()
            pref.summary = pref.entry ?: "${refreshRate} Hz"
        }

        allowBlurPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_BLUR, 0
        ) == 1

        allowHbmPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_HBM, 0
        ) == 1

        allowAodPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_AOD, 0
        ) == 1

        allowNightModeOffPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_NIGHT_MODE_OFF, 0
        ) == 1

        allowBrightnessPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_BRIGHTNESS, 0
        ) == 1

        // Performance
        allowLaunchBoostPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_LAUNCH_BOOST, 0
        ) == 1

        allowStandbyPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_STANDBY, 0
        ) == 1

        allowBackgroundPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_BACKGROUND, 0
        ) == 1

        disableQuickDozePref.isChecked = Settings.Global.getInt(
            resolver, KEY_DISABLE_QUICK_DOZE, 0
        ) == 1

        // Network
        allowNetworkPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_NETWORK, 0
        ) == 1

        allowDataPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_DATA, 0
        ) == 1

        // Sensors
        allowVibrationPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_VIBRATION, 0
        ) == 1

        allowSoundtriggerPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_SOUNDTRIGGER, 0
        ) == 1

        allowSensorsPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_SENSORS, 0
        ) == 1

        allowAttentionPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_ATTENTION, 0
        ) == 1

        allowCameraRotatePref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_CAMERA_ROTATE, 0
        ) == 1

        // Background tasks
        allowFullBackupPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_FULL_BACKUP, 0
        ) == 1

        allowKvBackupPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_KV_BACKUP, 0
        ) == 1

        // Location
        allowLocationPref.isChecked = Settings.Global.getInt(
            resolver, KEY_ALLOW_LOCATION, 0
        ) == 1
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val resolver = contentResolver

        return when (preference) {
            // Display
            allowAnimationPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_ANIMATION,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            refreshRatePref -> {
                val value = (newValue as String).toInt()
                Settings.Global.putInt(resolver, SETTING_REFRESH_RATE_CAP, value)
                refreshRatePref?.let { pref ->
                    val index = pref.findIndexOfValue(newValue)
                    pref.summary = pref.entries[index]
                }
                true
            }
            allowBlurPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_BLUR,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            allowHbmPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_HBM,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            allowAodPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_AOD,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            allowNightModeOffPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_NIGHT_MODE_OFF,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            allowBrightnessPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_BRIGHTNESS,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            // Performance
            allowLaunchBoostPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_LAUNCH_BOOST,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            allowStandbyPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_STANDBY,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            allowBackgroundPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_BACKGROUND,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            disableQuickDozePref -> {
                Settings.Global.putInt(
                    resolver, KEY_DISABLE_QUICK_DOZE,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            // Network
            allowNetworkPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_NETWORK,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            allowDataPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_DATA,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            // Sensors
            allowVibrationPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_VIBRATION,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            allowSoundtriggerPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_SOUNDTRIGGER,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            allowSensorsPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_SENSORS,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            allowAttentionPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_ATTENTION,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            allowCameraRotatePref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_CAMERA_ROTATE,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            // Background tasks
            allowFullBackupPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_FULL_BACKUP,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            allowKvBackupPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_KV_BACKUP,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            // Location
            allowLocationPref -> {
                Settings.Global.putInt(
                    resolver, KEY_ALLOW_LOCATION,
                    if (newValue as Boolean) 1 else 0
                )
                true
            }
            else -> false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(0, MENU_RESET, 0, R.string.battery_saver_options_reset)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_RESET -> {
                showResetDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showResetDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.battery_saver_options_reset_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                resetToDefaults()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun resetToDefaults() {
        val resolver = contentResolver

        // Reset all settings to defaults (0 = allow feature disabled)
        Settings.Global.putInt(resolver, KEY_ALLOW_ANIMATION, 0)
        // Refresh rate: 60 = 60Hz cap (default/most restrictive), 0 = no limit
        Settings.Global.putInt(resolver, SETTING_REFRESH_RATE_CAP, 60)
        Settings.Global.putInt(resolver, KEY_ALLOW_BLUR, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_HBM, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_AOD, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_NIGHT_MODE_OFF, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_BRIGHTNESS, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_LAUNCH_BOOST, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_STANDBY, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_BACKGROUND, 0)
        Settings.Global.putInt(resolver, KEY_DISABLE_QUICK_DOZE, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_NETWORK, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_DATA, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_VIBRATION, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_SOUNDTRIGGER, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_SENSORS, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_ATTENTION, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_CAMERA_ROTATE, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_FULL_BACKUP, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_KV_BACKUP, 0)
        Settings.Global.putInt(resolver, KEY_ALLOW_LOCATION, 0)

        // Reload UI
        loadSettings()
    }

    override fun getMetricsCategory(): Int = SettingsEnums.FUELGAUGE_BATTERY_SAVER

    companion object {
        private const val MENU_RESET = 1

        // Preference keys (also used as Settings.Global keys where matching)
        private const val KEY_ALLOW_ANIMATION = "battery_saver_allow_animation"
        private const val KEY_REFRESH_RATE = "battery_saver_refresh_rate"
        private const val KEY_ALLOW_BLUR = "battery_saver_allow_blur"
        private const val KEY_ALLOW_HBM = "battery_saver_allow_hbm"
        private const val KEY_ALLOW_AOD = "battery_saver_allow_aod"
        private const val KEY_ALLOW_NIGHT_MODE_OFF = "battery_saver_allow_night_mode_off"
        private const val KEY_ALLOW_BRIGHTNESS = "battery_saver_allow_brightness"
        private const val KEY_ALLOW_LAUNCH_BOOST = "battery_saver_allow_launch_boost"
        private const val KEY_ALLOW_STANDBY = "battery_saver_allow_standby"
        private const val KEY_ALLOW_BACKGROUND = "battery_saver_allow_background"
        private const val KEY_DISABLE_QUICK_DOZE = "battery_saver_disable_quick_doze"
        private const val KEY_ALLOW_NETWORK = "battery_saver_allow_network"
        private const val KEY_ALLOW_DATA = "battery_saver_allow_data"
        private const val KEY_ALLOW_VIBRATION = "battery_saver_allow_vibration"
        private const val KEY_ALLOW_SOUNDTRIGGER = "battery_saver_allow_soundtrigger"
        private const val KEY_ALLOW_SENSORS = "battery_saver_allow_sensors"
        private const val KEY_ALLOW_ATTENTION = "battery_saver_allow_attention"
        private const val KEY_ALLOW_CAMERA_ROTATE = "battery_saver_allow_camera_rotate"
        private const val KEY_ALLOW_FULL_BACKUP = "battery_saver_allow_full_backup"
        private const val KEY_ALLOW_KV_BACKUP = "battery_saver_allow_kv_backup"
        private const val KEY_ALLOW_LOCATION = "battery_saver_allow_location"

        // Settings.Global key (different from preference key)
        private const val SETTING_REFRESH_RATE_CAP = "battery_saver_refresh_rate_cap"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.battery_saver_options)
    }
}
