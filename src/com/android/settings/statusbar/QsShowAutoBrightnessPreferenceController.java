/*
 * Copyright (C) 2024 GrapheneOS
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
 * Preference controller for showing/hiding the auto brightness button in Quick Settings.
 */
public class QsShowAutoBrightnessPreferenceController extends TogglePreferenceController {

    public QsShowAutoBrightnessPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        return BenzeneSettings.Secure.getInt(mContext.getContentResolver(),
                BenzeneSettings.Secure.QS_SHOW_AUTO_BRIGHTNESS, 1) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return BenzeneSettings.Secure.putInt(mContext.getContentResolver(),
                BenzeneSettings.Secure.QS_SHOW_AUTO_BRIGHTNESS, isChecked ? 1 : 0);
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        // Only show on devices with automatic brightness support
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
