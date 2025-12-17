/*
 * Copyright (C) 2025 Amaan Qureshi <contact@amaanq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.statusbar

import android.content.Context
import app.benzeneos.providers.BenzeneSettings
import com.android.settings.core.TogglePreferenceController

/** Preference controller for showing/hiding the auto brightness button in Quick Settings. */
class QsShowAutoBrightnessPreferenceController(context: Context, key: String) :
    TogglePreferenceController(context, key) {

    override fun isChecked(): Boolean =
        BenzeneSettings.Secure.getInt(
            mContext.contentResolver,
            BenzeneSettings.Secure.QS_SHOW_AUTO_BRIGHTNESS,
            1
        ) == 1

    override fun setChecked(isChecked: Boolean): Boolean =
        BenzeneSettings.Secure.putInt(
            mContext.contentResolver,
            BenzeneSettings.Secure.QS_SHOW_AUTO_BRIGHTNESS,
            if (isChecked) 1 else 0
        )

    override fun getAvailabilityStatus(): Int =
        // Only show on devices with automatic brightness support
        if (mContext.resources.getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available
            )
        ) AVAILABLE else UNSUPPORTED_ON_DEVICE

    override fun getSliceHighlightMenuRes(): Int = 0
}
