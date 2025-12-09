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

package com.android.settings.display;

import static com.android.internal.display.RefreshRateSettingsUtils.DEFAULT_REFRESH_RATE;
import static com.android.internal.display.RefreshRateSettingsUtils.findHighestRefreshRateForDefaultDisplay;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class AppSpecificRefreshRatePreferenceController extends BasePreferenceController {

    private final DisplayManager mDisplayManager;
    private float mPeakRefreshRate;

    public AppSpecificRefreshRatePreferenceController(Context context, String key) {
        super(context, key);
        mDisplayManager = context.getSystemService(DisplayManager.class);
        mPeakRefreshRate = findHighestRefreshRateForDefaultDisplay(context);
    }

    @Override
    public int getAvailabilityStatus() {
        // Only show if device supports multiple refresh rates
        return mPeakRefreshRate > DEFAULT_REFRESH_RATE ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(R.string.app_specific_refresh_rate_summary);
    }
}
