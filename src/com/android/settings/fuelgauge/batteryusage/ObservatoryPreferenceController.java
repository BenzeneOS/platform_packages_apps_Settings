/*
 * Copyright (C) 2026 The BenzeneOS Authors
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

package com.android.settings.fuelgauge.batteryusage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import com.android.settings.core.BasePreferenceController;

public class ObservatoryPreferenceController extends BasePreferenceController {
    private static final ComponentName OBSERVATORY_COMPONENT =
            new ComponentName(
                    "org.benzeneos.observatory", "org.benzeneos.observatory.MainActivity");

    public ObservatoryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mContext.getUserId() != UserHandle.USER_SYSTEM) {
            return UNSUPPORTED_ON_DEVICE;
        }
        final Intent intent = new Intent(Intent.ACTION_MAIN).setComponent(OBSERVATORY_COMPONENT);
        return mContext.getPackageManager().resolveActivity(intent, 0) != null
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }
}
