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
import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.internal.view.RotationPolicy
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.core.lifecycle.LifecycleObserver
import com.android.settingslib.core.lifecycle.events.OnPause
import com.android.settingslib.core.lifecycle.events.OnResume

class AutoRotateEntryPreferenceController(context: Context, key: String) :
    TogglePreferenceController(context, key), LifecycleObserver, OnResume, OnPause {

    private val metricsFeatureProvider =
        FeatureFactory.featureFactory.metricsFeatureProvider
    private var preference: PrimarySwitchPreference? = null
    private var rotationPolicyListener: RotationPolicy.RotationPolicyListener? = null

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun getAvailabilityStatus(): Int =
        if (RotationPolicy.isRotationSupported(mContext)) AVAILABLE
        else UNSUPPORTED_ON_DEVICE

    override fun getSliceHighlightMenuRes(): Int = R.string.menu_key_display

    override fun isChecked(): Boolean =
        !RotationPolicy.isRotationLocked(mContext)

    override fun setChecked(isChecked: Boolean): Boolean {
        val isLocked = !isChecked
        metricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ROTATION_LOCK, isLocked)
        RotationPolicy.setRotationLock(
            mContext, isLocked,
            "AutoRotateEntryPreferenceController#setChecked"
        )
        return true
    }

    override fun onResume() {
        rotationPolicyListener = object : RotationPolicy.RotationPolicyListener() {
            override fun onChange() {
                preference?.let { updateState(it) }
            }
        }
        RotationPolicy.registerRotationPolicyListener(mContext, rotationPolicyListener)
    }

    override fun onPause() {
        rotationPolicyListener?.let {
            RotationPolicy.unregisterRotationPolicyListener(mContext, it)
        }
    }
}
