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

import android.content.Context
import android.provider.Settings
import android.widget.Toast
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.BasePreferenceController

class ResetAutoBrightnessPreferenceController(
    context: Context,
    key: String
) : BasePreferenceController(context, key), Preference.OnPreferenceClickListener {

    override fun getAvailabilityStatus(): Int =
        if (mContext.resources.getBoolean(com.android.internal.R.bool.config_automatic_brightness_available))
            AVAILABLE
        else
            UNSUPPORTED_ON_DEVICE

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        preference.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        Settings.System.putFloat(mContext.contentResolver, Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, 0f)
        Toast.makeText(mContext, R.string.reset_auto_brightness_done, Toast.LENGTH_SHORT).show()
        return true
    }
}
