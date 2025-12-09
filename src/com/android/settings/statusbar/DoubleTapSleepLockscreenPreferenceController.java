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
 * Preference controller for double tap to sleep on the lockscreen.
 */
public class DoubleTapSleepLockscreenPreferenceController extends TogglePreferenceController {

    public DoubleTapSleepLockscreenPreferenceController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        return BenzeneSettings.System.getInt(mContext.getContentResolver(),
                BenzeneSettings.System.DOUBLE_TAP_SLEEP_LOCKSCREEN, 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return BenzeneSettings.System.putInt(mContext.getContentResolver(),
                BenzeneSettings.System.DOUBLE_TAP_SLEEP_LOCKSCREEN, isChecked ? 1 : 0);
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
