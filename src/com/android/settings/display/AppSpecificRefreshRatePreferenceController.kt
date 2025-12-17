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
import com.android.internal.display.RefreshRateSettingsUtils.DEFAULT_REFRESH_RATE
import com.android.internal.display.RefreshRateSettingsUtils.findHighestRefreshRateForDefaultDisplay
import com.android.settings.R
import com.android.settings.core.BasePreferenceController

class AppSpecificRefreshRatePreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key) {

    private val peakRefreshRate: Float = findHighestRefreshRateForDefaultDisplay(context)

    override fun getAvailabilityStatus(): Int =
        // Only show if device supports multiple refresh rates
        if (peakRefreshRate > DEFAULT_REFRESH_RATE) AVAILABLE else UNSUPPORTED_ON_DEVICE

    override fun getSummary(): CharSequence =
        mContext.getString(R.string.app_specific_refresh_rate_summary)
}
