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

package com.android.settings.wifi

import android.content.Context
import android.provider.Settings
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController

/**
 * Preference controller for showing WiFi standard indicator in status bar.
 *
 * When enabled, displays the WiFi generation (4/5/6/7) next to the WiFi icon.
 */
class WifiStandardIconPreferenceController(
    context: Context,
    key: String
) : TogglePreferenceController(context, key) {

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun isChecked(): Boolean =
        Settings.System.getInt(mContext.contentResolver, Settings.System.WIFI_STANDARD_ICON, 0) == 1

    override fun setChecked(isChecked: Boolean): Boolean =
        Settings.System.putInt(mContext.contentResolver, Settings.System.WIFI_STANDARD_ICON, if (isChecked) 1 else 0)

    override fun getSliceHighlightMenuRes(): Int = R.string.menu_key_network
}
