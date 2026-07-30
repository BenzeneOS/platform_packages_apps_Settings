/*
 * Copyright (C) 2026 Amaan Qureshi <contact@amaanq.com>
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

package com.android.settings.notification.pushcompat

import android.content.Context
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.utils.ThreadUtils

class PushCompatOnDevicePreferenceController(
    context: Context,
    key: String,
    private var manager: PushCompatManager,
) : BasePreferenceController(context, key) {
    constructor(context: Context, key: String) : this(context, key, PushCompatManager(context))

    private var loadGeneration = 0

    override fun getAvailabilityStatus(): Int = AVAILABLE

    // Keep passive refreshes from disabling the switch.
    override fun updateState(preference: Preference) {
        val switchPreference = preference as SwitchPreferenceCompat
        val generation = ++loadGeneration
        ThreadUtils.postOnBackgroundThread {
            val status = manager.getStatus()
            val checked = status?.deliveryMode == PushCompatManager.DELIVERY_MODE_ON_DEVICE
            val enabled = status != null
            ThreadUtils.postOnMainThread {
                if (generation == loadGeneration) {
                    if (switchPreference.isChecked != checked) {
                        switchPreference.isChecked = checked
                    }
                    if (switchPreference.isEnabled != enabled) {
                        switchPreference.isEnabled = enabled
                    }
                }
            }
        }
        switchPreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, value ->
                switchPreference.isEnabled = false
                val mode =
                    if (value == true) {
                        PushCompatManager.DELIVERY_MODE_ON_DEVICE
                    } else {
                        PushCompatManager.DELIVERY_MODE_BRIDGE
                    }
                ThreadUtils.postOnBackgroundThread {
                    manager.setDeliveryMode(mode)
                    ThreadUtils.postOnMainThread { updateState(switchPreference) }
                }
                false
            }
    }

    fun setManager(manager: PushCompatManager) {
        this.manager = manager
    }
}
