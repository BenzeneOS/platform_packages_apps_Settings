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

package com.android.settings.sound

import android.content.Context
import android.provider.Settings
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController
import java.io.File

/**
 * Preference controller for Clear Calling feature.
 *
 * Clear Calling uses on-device AI to reduce background noise during phone calls.
 * This feature requires the libspeechenhancer.so library from Google's vendor partition.
 */
class ClearCallingPreferenceController(
    context: Context,
    key: String
) : TogglePreferenceController(context, key) {

    private val isAvailable: Boolean = File(SPEECH_ENHANCER_LIB_PATH).exists()

    override fun getAvailabilityStatus(): Int =
        if (isAvailable) AVAILABLE else UNSUPPORTED_ON_DEVICE

    override fun isChecked(): Boolean =
        Settings.Secure.getInt(mContext.contentResolver, SETTINGS_KEY, 1) == 1

    override fun setChecked(isChecked: Boolean): Boolean =
        Settings.Secure.putInt(mContext.contentResolver, SETTINGS_KEY, if (isChecked) 1 else 0)

    override fun getSummary(): CharSequence =
        mContext.getText(if (isAvailable) R.string.clear_calling_summary else R.string.clear_calling_unavailable)

    override fun getSliceHighlightMenuRes(): Int = R.string.menu_key_sound

    companion object {
        const val SETTINGS_KEY = "clear_calling_enabled"
        private const val SPEECH_ENHANCER_LIB_PATH = "/vendor/lib64/libspeechenhancer.so"
    }
}
