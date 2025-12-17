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

package com.android.settings.datetime

import android.content.Context
import android.provider.Settings
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController

class ClockSecondsQSPreferenceController(context: Context, preferenceKey: String) :
    TogglePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun isChecked(): Boolean =
        Settings.Secure.getInt(mContext.contentResolver, QS_HEADER_CLOCK_SECONDS, 0) != 0

    override fun setChecked(isChecked: Boolean): Boolean =
        Settings.Secure.putInt(
            mContext.contentResolver,
            QS_HEADER_CLOCK_SECONDS,
            if (isChecked) 1 else 0
        )

    override fun getSliceHighlightMenuRes(): Int = R.string.menu_key_system

    companion object {
        private const val QS_HEADER_CLOCK_SECONDS = "qs_header_clock_seconds"
    }
}
