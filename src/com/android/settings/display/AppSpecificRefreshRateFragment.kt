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
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.view.Display
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import kotlin.math.roundToInt

/** Fragment that shows app-specific refresh rate settings for a specific user profile */
class AppSpecificRefreshRateFragment : SettingsPreferenceFragment() {

    private lateinit var appListCategory: PreferenceCategory
    private lateinit var displayManager: DisplayManager
    private lateinit var supportedRefreshRates: FloatArray
    private var userId: Int = 0
    private var defaultPeakRefreshRate: Float = 60f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.app_specific_refresh_rate_settings)

        // Get the user ID from arguments (set by ProfileSelectFragment)
        userId = arguments?.getInt(Intent.EXTRA_USER_ID, UserHandle.myUserId())
            ?: UserHandle.myUserId()

        displayManager = requireContext().getSystemService(DisplayManager::class.java)
        appListCategory = findPreference(KEY_APP_LIST)!!

        loadSupportedRefreshRates()
        populateAppList()
    }

    override fun getMetricsCategory(): Int = SettingsEnums.DISPLAY

    private fun loadSupportedRefreshRates() {
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        if (display == null) {
            Log.e(TAG, "Could not get default display")
            supportedRefreshRates = floatArrayOf(60f)
            defaultPeakRefreshRate = 60f
            return
        }

        val modes = display.supportedModes
        val refreshRates = mutableSetOf<Float>()
        var maxRefreshRate = 60f

        for (mode in modes) {
            val rate = mode.refreshRate
            refreshRates.add(rate)
            if (rate > maxRefreshRate) {
                maxRefreshRate = rate
            }
        }
        // Add 90Hz if it's not already in the list
        if (!refreshRates.contains(90f)) {
            refreshRates.add(90f)
        }

        defaultPeakRefreshRate = maxRefreshRate

        val sortedRates = refreshRates.sorted()
        supportedRefreshRates = sortedRates.toFloatArray()

        Log.d(TAG, "Supported refresh rates: $sortedRates, max: $maxRefreshRate")
    }

    private fun populateAppList() {
        val context = requireContext()
        val pm = context.packageManager

        // Get apps for the specific user
        val apps: List<ApplicationInfo> = try {
            pm.getInstalledApplicationsAsUser(PackageManager.GET_META_DATA, userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get apps for user $userId", e)
            return
        }

        // Filter to only show user-installed apps and system apps with launchers
        val filteredApps = apps.filter { app ->
            (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                pm.getLaunchIntentForPackage(app.packageName) != null
        }

        // Sort alphabetically
        val sortedApps = filteredApps.sortedWith { a, b ->
            val labelA = pm.getApplicationLabel(a).toString()
            val labelB = pm.getApplicationLabel(b).toString()
            labelA.compareTo(labelB, ignoreCase = true)
        }

        // Create preferences for each app
        for (app in sortedApps) {
            createAppPreference(app, pm)
        }
    }

    private fun createAppPreference(app: ApplicationInfo, pm: PackageManager) {
        val pref = ListPreference(requireContext()).apply {
            key = app.packageName
            title = pm.getApplicationLabel(app)

            // Set app icon
            try {
                icon = pm.getApplicationIcon(app)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load icon for ${app.packageName}", e)
            }
        }

        // Get app's actual default refresh rate
        val appDefaultRate = getAppDefaultRefreshRate(app)

        // Build entries: Default + all supported refresh rates
        val entries = arrayOfNulls<CharSequence>(supportedRefreshRates.size + 1)
        val values = arrayOfNulls<CharSequence>(supportedRefreshRates.size + 1)

        entries[0] = "$appDefaultRate Hz ${getString(R.string.app_refresh_rate_default_suffix)}"
        values[0] = "0"

        for (i in supportedRefreshRates.indices) {
            val rate = supportedRefreshRates[i].roundToInt()
            entries[i + 1] = "$rate Hz"
            values[i + 1] = rate.toString()
        }

        pref.entries = entries
        pref.entryValues = values

        // Load current value
        val currentRate = getAppRefreshRate(app.packageName)
        if (currentRate == 0f) {
            pref.value = "0"
            pref.summary = "$appDefaultRate Hz ${getString(R.string.app_refresh_rate_default_suffix)}"
        } else {
            val rate = currentRate.roundToInt()
            pref.value = rate.toString()
            pref.summary = "$rate Hz"
        }

        pref.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue as String
            val rate = value.toFloat()
            setAppRefreshRate(app.packageName, rate)

            pref.summary = if (rate == 0f) {
                "$appDefaultRate Hz ${getString(R.string.app_refresh_rate_default_suffix)}"
            } else {
                "${rate.roundToInt()} Hz"
            }
            true
        }

        appListCategory.addPreference(pref)
    }

    private fun getAppDefaultRefreshRate(app: ApplicationInfo): Int {
        // Check if app is on the high refresh rate denylist
        val denylist = requireContext().resources.getStringArray(
            com.android.internal.R.array.config_highRefreshRateBlacklist
        )
        for (pkg in denylist) {
            if (pkg == app.packageName) {
                // Denylisted apps get 60Hz as default
                return 60
            }
        }

        // Known apps that programmatically limit refresh rate
        when (app.packageName) {
            "com.google.android.apps.maps" -> return 60
            // Add more apps as discovered
        }

        // Default to system peak refresh rate
        return defaultPeakRefreshRate.roundToInt()
    }

    private fun getAppRefreshRate(packageName: String): Float {
        // Store settings per-user by using user-specific setting key
        // For user 0 (main user), use the base key without suffix for framework compatibility
        val settingKey = if (userId == 0) SETTING_KEY else "${SETTING_KEY}_$userId"
        val allSettings = Settings.System.getString(
            requireContext().contentResolver, settingKey
        )

        if (allSettings.isNullOrEmpty()) {
            return 0f
        }

        val entries = allSettings.split(";")
        for (entry in entries) {
            val parts = entry.split(":")
            if (parts.size == 2 && parts[0] == packageName) {
                return try {
                    parts[1].toFloat()
                } catch (e: NumberFormatException) {
                    Log.e(TAG, "Invalid refresh rate for $packageName", e)
                    0f
                }
            }
        }
        return 0f
    }

    private fun setAppRefreshRate(packageName: String, refreshRate: Float) {
        // Store settings per-user by using user-specific setting key
        // For user 0 (main user), use the base key without suffix for framework compatibility
        val settingKey = if (userId == 0) SETTING_KEY else "${SETTING_KEY}_$userId"
        val allSettings = Settings.System.getString(
            requireContext().contentResolver, settingKey
        )

        val newSettings = StringBuilder()
        var found = false

        if (!allSettings.isNullOrEmpty()) {
            val entries = allSettings.split(";")
            for (entry in entries) {
                val parts = entry.split(":")
                if (parts.size == 2) {
                    if (parts[0] == packageName) {
                        found = true
                        if (refreshRate > 0) {
                            if (newSettings.isNotEmpty()) newSettings.append(";")
                            newSettings.append(packageName).append(":").append(refreshRate)
                        }
                    } else {
                        if (newSettings.isNotEmpty()) newSettings.append(";")
                        newSettings.append(entry)
                    }
                }
            }
        }

        if (!found && refreshRate > 0) {
            if (newSettings.isNotEmpty()) newSettings.append(";")
            newSettings.append(packageName).append(":").append(refreshRate)
        }

        Settings.System.putString(
            requireContext().contentResolver, settingKey, newSettings.toString()
        )

        Log.d(TAG, "Set $packageName to $refreshRate Hz for user $userId")
    }

    companion object {
        private const val TAG = "AppSpecRefreshRateFrag"
        private const val KEY_APP_LIST = "app_refresh_rate_list"
        private const val SETTING_KEY = "app_specific_refresh_rates"
    }
}
