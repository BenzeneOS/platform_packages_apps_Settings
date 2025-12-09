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

package com.android.settings.dashboard.profileselector;

import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.display.AppSpecificRefreshRateFragment;

import java.util.List;

/**
 * Settings page for configuring per-app refresh rates with support for Personal and Private spaces
 */
public class AppSpecificRefreshRateSettings extends ProfileSelectFragment {

    private static final String TAG = "AppSpecRefreshRateSet";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Debug: Check what profiles exist
        UserManager userManager = getContext().getSystemService(UserManager.class);
        List<UserInfo> profiles = userManager.getProfiles(UserHandle.myUserId());
        Log.d(TAG, "=== Profile Debug Info ===");
        Log.d(TAG, "Total profiles: " + profiles.size());
        for (UserInfo profile : profiles) {
            Log.d(TAG, "Profile: id=" + profile.id + " name=" + profile.name +
                  " isMain=" + profile.isMain() +
                  " isManagedProfile=" + profile.isManagedProfile() +
                  " isPrivateProfile=" + profile.isPrivateProfile());
        }
        Log.d(TAG, "Flags.allowPrivateProfile=" + android.os.Flags.allowPrivateProfile());
        Log.d(TAG, "multiuser.Flags.enablePrivateSpaceFeatures=" +
              android.multiuser.Flags.enablePrivateSpaceFeatures());
    }

    @Override
    public Fragment[] getFragments() {
        Fragment[] fragments = ProfileSelectFragment.getFragments(
                getContext(),
                getArguments(),
                AppSpecificRefreshRateFragment::new,
                AppSpecificRefreshRateFragment::new,
                AppSpecificRefreshRateFragment::new);
        Log.d(TAG, "getFragments() returned " + fragments.length + " fragments");
        return fragments;
    }

    @Override
    public int getTitleResId() {
        return R.string.app_specific_refresh_rate_title;
    }
}
