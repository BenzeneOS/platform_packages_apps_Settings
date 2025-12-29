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

package com.android.settings.applications.appinfo

import android.content.Context
import android.content.pm.GosPackageState
import android.os.Process
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.applications.BatterySpoofFragment
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.applications.AppUtils
import com.android.settingslib.core.lifecycle.Lifecycle
import com.android.settingslib.core.lifecycle.LifecycleObserver
import com.android.settingslib.core.lifecycle.events.OnResume

class BatterySpoofPreferenceController(
    context: Context,
    private val parent: AppInfoDashboardFragment,
    private val packageName: String,
    private val uid: Int,
    lifecycle: Lifecycle?
) : BasePreferenceController(context, KEY_BATTERY_SPOOF), LifecycleObserver, OnResume {

    private val userId: Int = context.userId
    private var preference: Preference? = null

    init {
        lifecycle?.addObserver(this)
    }

    override fun getAvailabilityStatus(): Int =
        if (uid < Process.FIRST_APPLICATION_UID) CONDITIONALLY_UNAVAILABLE else AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
        if (!AppUtils.isAppInstalled(parent.appEntry)) {
            preference?.summary = ""
            return
        }
        preference?.let { updateState(it) }
    }

    override fun onResume() {
        preference?.let { updateState(it) }
    }

    override fun updateState(preference: Preference) {
        val ps = GosPackageState.get(packageName, userId)
        val spoofedLevel = ps?.spoofedBatteryLevel ?: -1

        preference.summary = when {
            spoofedLevel == -1 -> mContext.getString(R.string.battery_spoof_summary_global)
            spoofedLevel == -2 -> mContext.getString(R.string.battery_spoof_summary_real)
            else -> mContext.getString(R.string.battery_spoof_summary_spoofed, spoofedLevel)
        }
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (KEY_BATTERY_SPOOF != preference.key) {
            return false
        }
        BatterySpoofFragment.show(parent, packageName, userId)
        return true
    }

    companion object {
        private const val KEY_BATTERY_SPOOF = "battery_spoof"
    }
}
