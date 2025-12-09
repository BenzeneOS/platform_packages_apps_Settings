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
package com.android.settings.statusbar;

import android.content.Context;

import androidx.annotation.NonNull;

import app.benzeneos.providers.BenzeneSettings;

import com.android.settings.core.TogglePreferenceController;

/**
 * Master preference controller for double tap to sleep.
 * This controls both status bar and lockscreen double tap to sleep at once.
 */
public class DoubleTapSleepPreferenceController extends TogglePreferenceController {

    public DoubleTapSleepPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        // Master is on if either is enabled
        int statusBar = BenzeneSettings.System.getInt(mContext.getContentResolver(),
                BenzeneSettings.System.DOUBLE_TAP_SLEEP_STATUS_BAR, 0);
        int lockscreen = BenzeneSettings.System.getInt(mContext.getContentResolver(),
                BenzeneSettings.System.DOUBLE_TAP_SLEEP_LOCKSCREEN, 0);
        return statusBar == 1 || lockscreen == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        // Master toggle enables/disables both at once
        int value = isChecked ? 1 : 0;
        boolean statusBarResult = BenzeneSettings.System.putInt(mContext.getContentResolver(),
                BenzeneSettings.System.DOUBLE_TAP_SLEEP_STATUS_BAR, value);
        boolean lockscreenResult = BenzeneSettings.System.putInt(mContext.getContentResolver(),
                BenzeneSettings.System.DOUBLE_TAP_SLEEP_LOCKSCREEN, value);
        return statusBarResult && lockscreenResult;
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
