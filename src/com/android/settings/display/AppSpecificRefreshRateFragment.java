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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragment that shows app-specific refresh rate settings for a specific user profile
 */
public class AppSpecificRefreshRateFragment extends SettingsPreferenceFragment {
    private static final String TAG = "AppSpecRefreshRateFrag";
    private static final String KEY_APP_LIST = "app_refresh_rate_list";
    private static final String SETTING_KEY = "app_specific_refresh_rates";

    private PreferenceCategory mAppListCategory;
    private DisplayManager mDisplayManager;
    private float[] mSupportedRefreshRates;
    private int mUserId;
    private float mDefaultPeakRefreshRate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_specific_refresh_rate_settings);

        // Get the user ID from arguments (set by ProfileSelectFragment)
        Bundle args = getArguments();
        if (args != null) {
            mUserId = args.getInt(android.content.Intent.EXTRA_USER_ID, UserHandle.myUserId());
        } else {
            mUserId = UserHandle.myUserId();
        }

        mDisplayManager = getContext().getSystemService(DisplayManager.class);
        mAppListCategory = findPreference(KEY_APP_LIST);

        loadSupportedRefreshRates();
        populateAppList();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DISPLAY;
    }

    private void loadSupportedRefreshRates() {
        Display display = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            Log.e(TAG, "Could not get default display");
            mSupportedRefreshRates = new float[]{60.0f};
            mDefaultPeakRefreshRate = 60.0f;
            return;
        }

        Display.Mode[] modes = display.getSupportedModes();
        Set<Float> refreshRates = new HashSet<>();
        float maxRefreshRate = 60.0f;

        for (Display.Mode mode : modes) {
            float rate = mode.getRefreshRate();
            refreshRates.add(rate);
            if (rate > maxRefreshRate) {
                maxRefreshRate = rate;
            }
        }
        // Add 90Hz if it's not already in the list
        if (!refreshRates.contains(90.0f)) {
            refreshRates.add(90.0f);
        }

        mDefaultPeakRefreshRate = maxRefreshRate;

        List<Float> sortedRates = new ArrayList<>(refreshRates);
        Collections.sort(sortedRates);

        mSupportedRefreshRates = new float[sortedRates.size()];
        for (int i = 0; i < sortedRates.size(); i++) {
            mSupportedRefreshRates[i] = sortedRates.get(i);
        }

        Log.d(TAG, "Supported refresh rates: " + sortedRates + ", max: " + maxRefreshRate);
    }

    private void populateAppList() {
        Context context = getContext();
        PackageManager pm = context.getPackageManager();

        // Get apps for the specific user
        List<ApplicationInfo> apps;
        try {
            apps = pm.getInstalledApplicationsAsUser(
                    PackageManager.GET_META_DATA, mUserId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get apps for user " + mUserId, e);
            return;
        }

        // Filter to only show user-installed apps and system apps with launchers
        List<ApplicationInfo> filteredApps = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 ||
                pm.getLaunchIntentForPackage(app.packageName) != null) {
                filteredApps.add(app);
            }
        }

        // Sort alphabetically
        Collections.sort(filteredApps, (a, b) -> {
            String labelA = pm.getApplicationLabel(a).toString();
            String labelB = pm.getApplicationLabel(b).toString();
            return labelA.compareToIgnoreCase(labelB);
        });

        // Create preferences for each app
        for (ApplicationInfo app : filteredApps) {
            createAppPreference(app, pm);
        }
    }

    private void createAppPreference(ApplicationInfo app, PackageManager pm) {
        ListPreference pref = new ListPreference(getContext());
        pref.setKey(app.packageName);
        pref.setTitle(pm.getApplicationLabel(app));

        // Set app icon
        try {
            pref.setIcon(pm.getApplicationIcon(app));
        } catch (Exception e) {
            Log.e(TAG, "Failed to load icon for " + app.packageName, e);
        }

        // Get app's actual default refresh rate
        int appDefaultRate = getAppDefaultRefreshRate(app);

        // Build entries: Default + all supported refresh rates
        CharSequence[] entries = new CharSequence[mSupportedRefreshRates.length + 1];
        CharSequence[] values = new CharSequence[mSupportedRefreshRates.length + 1];

        entries[0] = appDefaultRate + " Hz " + getString(R.string.app_refresh_rate_default_suffix);
        values[0] = "0";

        for (int i = 0; i < mSupportedRefreshRates.length; i++) {
            int rate = Math.round(mSupportedRefreshRates[i]);
            entries[i + 1] = rate + " Hz";
            values[i + 1] = String.valueOf(rate);
        }

        pref.setEntries(entries);
        pref.setEntryValues(values);

        // Load current value
        float currentRate = getAppRefreshRate(app.packageName);
        if (currentRate == 0) {
            pref.setValue("0");
            pref.setSummary(appDefaultRate + " Hz " + getString(R.string.app_refresh_rate_default_suffix));
        } else {
            int rate = Math.round(currentRate);
            pref.setValue(String.valueOf(rate));
            pref.setSummary(rate + " Hz");
        }

        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            String value = (String) newValue;
            float rate = Float.parseFloat(value);
            setAppRefreshRate(app.packageName, rate);

            if (rate == 0) {
                pref.setSummary(appDefaultRate + " Hz " + getString(R.string.app_refresh_rate_default_suffix));
            } else {
                pref.setSummary(Math.round(rate) + " Hz");
            }
            return true;
        });

        mAppListCategory.addPreference(pref);
    }

    private int getAppDefaultRefreshRate(ApplicationInfo app) {
        // Check if app is on the high refresh rate denylist
        String[] denylist = getContext().getResources().getStringArray(
                com.android.internal.R.array.config_highRefreshRateBlacklist);
        for (String pkg : denylist) {
            if (pkg.equals(app.packageName)) {
                // Denylisted apps get 60Hz as default
                return 60;
            }
        }

        // Known apps that programmatically limit refresh rate
        switch (app.packageName) {
            case "com.google.android.apps.maps":
                return 60;
            // Add more apps as discovered
        }

        // Default to system peak refresh rate
        return Math.round(mDefaultPeakRefreshRate);
    }

    private float getAppRefreshRate(String packageName) {
        // Store settings per-user by using user-specific setting key
        // For user 0 (main user), use the base key without suffix for framework compatibility
        String settingKey = (mUserId == 0) ? SETTING_KEY : SETTING_KEY + "_" + mUserId;
        String allSettings = Settings.System.getString(
                getContext().getContentResolver(), settingKey);

        if (allSettings == null || allSettings.isEmpty()) {
            return 0;
        }

        String[] entries = allSettings.split(";");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length == 2 && parts[0].equals(packageName)) {
                try {
                    return Float.parseFloat(parts[1]);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid refresh rate for " + packageName, e);
                    return 0;
                }
            }
        }
        return 0;
    }

    private void setAppRefreshRate(String packageName, float refreshRate) {
        // Store settings per-user by using user-specific setting key
        // For user 0 (main user), use the base key without suffix for framework compatibility
        String settingKey = (mUserId == 0) ? SETTING_KEY : SETTING_KEY + "_" + mUserId;
        String allSettings = Settings.System.getString(
                getContext().getContentResolver(), settingKey);

        StringBuilder newSettings = new StringBuilder();
        boolean found = false;

        if (allSettings != null && !allSettings.isEmpty()) {
            String[] entries = allSettings.split(";");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    if (parts[0].equals(packageName)) {
                        found = true;
                        if (refreshRate > 0) {
                            if (newSettings.length() > 0) newSettings.append(";");
                            newSettings.append(packageName).append(":").append(refreshRate);
                        }
                    } else {
                        if (newSettings.length() > 0) newSettings.append(";");
                        newSettings.append(entry);
                    }
                }
            }
        }

        if (!found && refreshRate > 0) {
            if (newSettings.length() > 0) newSettings.append(";");
            newSettings.append(packageName).append(":").append(refreshRate);
        }

        Settings.System.putString(
                getContext().getContentResolver(), settingKey, newSettings.toString());

        Log.d(TAG, "Set " + packageName + " to " + refreshRate + " Hz for user " + mUserId);
    }
}
